package dataStreamTransform;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoProcessFunction;
import org.apache.flink.util.Collector;

/**
 * @author haun
 * @version 1.0.0
 * @date 2022/11/6 13:29
 */
public class BillCheckExample {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // ���� app ��֧����־
        SingleOutputStreamOperator<Tuple3<String, String, Long>> appStream = env.fromElements(Tuple3.of("order-1", "app", 1000L),
                Tuple3.of("order-2", "app", 2000L)
        ).assignTimestampsAndWatermarks(WatermarkStrategy.<Tuple3<String, String, Long>>forMonotonousTimestamps()
                .withTimestampAssigner(new SerializableTimestampAssigner<Tuple3<String, String, Long>>() {
                    @Override
                    public long extractTimestamp(Tuple3<String, String, Long> element, long recordTimestamp) {
                        return element.f2;
                    }
                }));

        // ���Ե�����֧��ƽ̨��֧����־
        SingleOutputStreamOperator<Tuple4<String, String, String, Long>>
                thirdpartStream = env.fromElements(
                Tuple4.of("order-1", "third-party", "success", 3000L),
                Tuple4.of("order-3", "third-party", "success", 4000L)
        ).assignTimestampsAndWatermarks(WatermarkStrategy.<Tuple4<String, String, String, Long>>forMonotonousTimestamps()
                        .withTimestampAssigner(new
                                                       SerializableTimestampAssigner<Tuple4<String, String, String, Long>>() {
                                                           @Override
                                                           public long extractTimestamp(Tuple4<String, String, String, Long>
                                                                                                element, long recordTimestamp) {
                                                               return element.f3;
                                                           }
                                                       })
        );


        appStream.connect(thirdpartStream)
                .keyBy(data -> data.f0,data -> data.f0)
                .process(new OrderMatchResult())
                .print();

        env.execute();

    }

    public static class OrderMatchResult extends CoProcessFunction<Tuple3<String, String, Long>, Tuple4<String, String, String, Long>, String> {
        // ����״̬���������������Ѿ�������¼�
        private ValueState<Tuple3<String, String, Long>> appEventState;
        private ValueState<Tuple4<String, String, String, Long>> thirdPartyEventState;

        @Override
        public void open(Configuration parameters) throws Exception {
            appEventState = getRuntimeContext().getState(
            new ValueStateDescriptor<Tuple3<String, String, Long>>("app-event", Types.TUPLE(Types.STRING, Types.STRING, Types.LONG)));
            thirdPartyEventState = getRuntimeContext().getState(
                    new ValueStateDescriptor<Tuple4<String, String, String, Long>>("thirdparty-event", Types.TUPLE(Types.STRING, Types.STRING, Types.STRING,Types.LONG)));
        }

        @Override
        public void processElement1(Tuple3<String, String, Long> value, Context ctx, Collector<String> out) throws Exception {
            // ����һ�������¼��Ƿ�����
            if (thirdPartyEventState.value() != null){
                out.collect(" �� �� �� �� �� " + value + " " +
                        thirdPartyEventState.value());
                // ���״̬
                thirdPartyEventState.clear();
            } else {
                // ����״̬
                appEventState.update(value);
                // ע��һ�� 5 ���Ķ�ʱ������ʼ�ȴ���һ�������¼�
                ctx.timerService().registerEventTimeTimer(value.f2 + 5000L);
            }
        }

        @Override
        public void processElement2(Tuple4<String, String, String, Long> value, Context ctx, Collector<String> out) throws Exception {
            if (appEventState.value() != null){
                out.collect("���˳ɹ���" + appEventState.value() + " " + value);
                // ���״̬
                appEventState.clear();
            } else {
                // ����״̬
                thirdPartyEventState.update(value);
                // ע��һ�� 5 ���Ķ�ʱ������ʼ�ȴ���һ�������¼�
                ctx.timerService().registerEventTimeTimer(value.f3 + 5000L);
            }
        }

        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
            // ��ʱ���������ж�״̬�����ĳ��״̬��Ϊ�գ�˵����һ�������¼�û��
            if (appEventState.value() != null) {
                out.collect("����ʧ�ܣ�" + appEventState.value() + " " + "������֧��ƽ̨��Ϣδ��");
            }
            if (thirdPartyEventState.value() != null) {
                out.collect("����ʧ�ܣ�" + thirdPartyEventState.value() + " " + "app��Ϣδ��");
            }
            appEventState.clear();
            thirdPartyEventState.clear();
        }


    }



}
