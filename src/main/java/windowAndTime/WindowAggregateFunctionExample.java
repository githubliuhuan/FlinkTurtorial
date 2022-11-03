package windowAndTime;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author liuhuan
 * @date 2022-11-03
 */
public class WindowAggregateFunctionExample {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

//        env.addSource(new ClickSource())
//                .assignTimestampsAndWatermarks(WatermarkStrategy.forMonotonousTimestamps())

    }
}
