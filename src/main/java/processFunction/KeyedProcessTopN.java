package processFunction;

import dataStreamApi.ClickSource;
import dataStreamApi.Event;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import windowAndTime.UrlViewCount;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * @author haun
 * @version 1.0.0
 * @date 2022/11/5 12:26
 */
public class KeyedProcessTopN {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // ���Զ�������Դ��ȡ����
        SingleOutputStreamOperator<Event> eventStream = env.addSource(new
                ClickSource())
                .assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forMonotonousTimestamps()
                        .withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                                                                   @Override
                                                                   public long extractTimestamp(Event element, long
                                                                           recordTimestamp) {
                                                                       return element.timestamp;
                                                                   }
                                                               }));

        // ��Ҫ���� url ���飬���ÿ�� url �ķ�����
        SingleOutputStreamOperator<UrlViewCount> urlCountStream =
                eventStream.keyBy(data -> data.url)
                        .window(SlidingEventTimeWindows.of(Time.seconds(10),
                                Time.seconds(5)))
                        .aggregate(new UrlViewCountAgg(),
                                new UrlViewCountResult());

        // �Խ����ͬһ�����ڵ�ͳ�����ݣ�����������
        SingleOutputStreamOperator<String> result = urlCountStream.keyBy(data ->
                data.windowEnd)
                .process(new TopN(2));
        result.print("result");
        env.execute();
    }

    // �Զ��������ۺ�
    public static class UrlViewCountAgg implements AggregateFunction<Event, Long, Long> {
        @Override
        public Long createAccumulator() {
            return 0L;
        }
        @Override
        public Long add(Event value, Long accumulator) {
            return accumulator + 1;
        }
        @Override
        public Long getResult(Long accumulator) {
            return accumulator;
        }
        @Override
        public Long merge(Long a, Long b) {
            return null;
        }
    }

    // �Զ���ȫ���ں�����ֻ��Ҫ��װ������Ϣ
    public static class UrlViewCountResult extends ProcessWindowFunction<Long,
                UrlViewCount, String, TimeWindow> {
        @Override
        public void process(String url, Context context, Iterable<Long> elements,
                            Collector<UrlViewCount> out) throws Exception {
            // ��ϴ�����Ϣ����װ�������
            Long start = context.window().getStart();
            Long end = context.window().getEnd();
            out.collect(new UrlViewCount(url, elements.iterator().next(), start,
                    end));
        }
    }

    // �Զ��崦����������ȡ top n
    public static class TopN extends KeyedProcessFunction<Long, UrlViewCount,
                String> {
        // �� n ��Ϊ����
        private Integer n;
        // ����һ���б�״̬
        private ListState<UrlViewCount> urlViewCountListState;
        public TopN(Integer n) {
            this.n = n;
        }
        @Override
        public void open(Configuration parameters) throws Exception {
            // �ӻ����л�ȡ�б�״̬���
            urlViewCountListState = getRuntimeContext().getListState(
                    new ListStateDescriptor<UrlViewCount>("url-view-count-list",
                            Types.POJO(UrlViewCount.class)));
        }
        @Override
        public void processElement(UrlViewCount value, Context ctx,
                                   Collector<String> out) throws Exception {
            // �� count ������ӵ��б�״̬�У���������
            urlViewCountListState.add(value);
            // ע�� window end + 1ms ��Ķ�ʱ�����ȴ��������ݵ��뿪ʼ����
            ctx.timerService().registerEventTimeTimer(ctx.getCurrentKey() + 1);
        }
        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<String>
                out) throws Exception {
            // �����ݴ��б�״̬������ȡ�������� ArrayList����������
            ArrayList<UrlViewCount> urlViewCountArrayList = new ArrayList<>();
            for (UrlViewCount urlViewCount : urlViewCountListState.get()) {
                urlViewCountArrayList.add(urlViewCount);
            }
            // ���״̬���ͷ���Դ
            urlViewCountListState.clear();
            // ����
            urlViewCountArrayList.sort(new Comparator<UrlViewCount>() {
                @Override
                public int compare(UrlViewCount o1, UrlViewCount o2) {
                    return o2.count.intValue() - o1.count.intValue();
                }
            });
            // ȡǰ����������������
            StringBuilder result = new StringBuilder();
            result.append("========================================\n");
            result.append("���ڽ���ʱ�䣺" + new Timestamp(timestamp - 1) + "\n");
            for (int i = 0; i < this.n; i++) {
                UrlViewCount UrlViewCount = urlViewCountArrayList.get(i);
                String info = "No." + (i + 1) + " "
                        + "url��" + UrlViewCount.url + " "
                        + "�������" + UrlViewCount.count + "\n";
                result.append(info);
            }
            result.append("========================================\n");
            out.collect(result.toString());
        }
    }
}