package processFunction;

import dataStreamApi.ClickSource;
import dataStreamApi.Event;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import scala.collection.mutable.StringBuilder;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * @author haun
 * @version 1.0.0
 * @date 2022/11/5 11:36
 */
public class ProcessAllWindowTopN {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        SingleOutputStreamOperator<Event> eventStream = env.addSource(new ClickSource())
                .assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forMonotonousTimestamps()
                        .withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                            @Override
                            public long extractTimestamp(Event event, long l) {
                                return event.timestamp;
                            }
                        }));

        // ֻ��Ҫ url �Ϳ���ͳ������������ת���� String ֱ�ӿ���ͳ��
        SingleOutputStreamOperator<String> result = eventStream.map(new MapFunction<Event, String>() {
            @Override
            public String map(Event event) throws Exception {
                return event.url;
            }
        })
                .windowAll(SlidingEventTimeWindows.of(Time.seconds(10), Time.seconds(5))) // ����������
                .process(new ProcessAllWindowFunction<String, String, TimeWindow>() {
                    @Override
                    public void process(Context context, Iterable<String> elements, Collector<String> out) throws Exception {
                        HashMap<String, Long> urlCountMap = new HashMap<>();
                        // �������������ݣ�����������浽һ��HashMap��
                        for (String url : elements) {
                            if (urlCountMap.containsKey(url)) {
                                Long count = urlCountMap.get(url);
                                urlCountMap.put(url, count + 1L);
                            } else {
                                urlCountMap.put(url, 1L);
                            }
                        }

                        ArrayList<Tuple2<String, Long>> mapList = new ArrayList<Tuple2<String, Long>>();
                        // ����������ݷ��� ArrayList����������
                        for (String key : urlCountMap.keySet()) {
                            mapList.add(Tuple2.of(key, urlCountMap.get(key)));
                        }
                        mapList.sort(new Comparator<Tuple2<String, Long>>() {
                            @Override
                            public int compare(Tuple2<String, Long> o1, Tuple2<String, Long> o2) {
                                return o2.f1.intValue() - o1.f1.intValue();
                            }
                        });

                        // ȡ������ǰ����������������
                        StringBuilder result = new StringBuilder();
                        result.append("========================================\n");

                        for (int i = 0; i < 2; i++) {
                            Tuple2<String, Long> temp = mapList.get(i);
                            String info = "�����No." + (i + 1) +
                                    "url��" + temp.f0 +
                                    "�������" + temp.f1 +
                                    "���ڽ���ʱ�䣺" + new Timestamp(context.window().getEnd()) + "\n";
                            result.append(info);
                        }
                        result.append("========================================\n");
                        out.collect(result.toString());
                    }
                });

        result.print();

        env.execute();

    }
}
