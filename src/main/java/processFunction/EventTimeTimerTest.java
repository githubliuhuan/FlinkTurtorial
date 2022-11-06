package processFunction;

import dataStreamApi.Event;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.util.Collector;

/**
 * @author haun
 * @version 1.0.0
 * @date 2022/11/5 10:27
 */
public class EventTimeTimerTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        SingleOutputStreamOperator<Event> stream = env.addSource(new CustomSource())
                .assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forMonotonousTimestamps()
                        .withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                            @Override
                            public long extractTimestamp(Event event, long l) {
                                return event.timestamp;
                            }
                        }));

        // ����KeyedStream�����¼��¼���ʱ��
        stream.keyBy(data -> true)
                .process(new KeyedProcessFunction<Boolean, Event, String>() {
                    @Override
                    public void processElement(Event event, Context ctx, Collector<String> out) throws Exception {
                        out.collect("���ݵ��ʱ���Ϊ��" + ctx.timestamp());
                        out.collect(" ���ݵ��ˮλ��Ϊ�� " +
                                ctx.timerService().currentWatermark() + "\n -------�ָ���-------");
                        // ע��һ�� 10 ���Ķ�ʱ��
                        ctx.timerService().registerEventTimeTimer(ctx.timestamp()
                                + 10 * 1000L);
                    }

                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
                        out.collect("��ʱ������������ʱ�䣺" + timestamp);
                    }
                }).print();

        env.execute();
    }

    // �Զ����������Դ
    public static class CustomSource implements SourceFunction<Event> {
        @Override
        public void run(SourceContext<Event> ctx) throws Exception {
            // ֱ�ӷ�����������
            ctx.collect(new Event("Mary", "./home", 1000L));
            // Ϊ�˸������ԣ��м�ͣ�� 5 ����
            Thread.sleep(5000L);
            // ���� 10 ��������
            ctx.collect(new Event("Mary", "./home", 11000L));
            Thread.sleep(5000L);
            // ���� 10 ��+1ms �������
            ctx.collect(new Event("Alice", "./cart", 11001L));
            Thread.sleep(5000L);
        }
        @Override
        public void cancel() { }
    }

}
