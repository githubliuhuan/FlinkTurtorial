package dataStreamApi;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author liuhuan
 * @date 2022-11-02
 */
public class TransTupleAggreationTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Tuple3<String, Integer,Integer>> stream = env.fromElements( Tuple3.of("a", 1,2),
                Tuple3.of("a", 3,2), Tuple3.of("b", 3,1),Tuple3.of("b", 4,5)
                );

//        stream.keyBy(r -> r.f0).sum(1).print();
        stream.keyBy(r -> r.f0).min(1).print();
//        stream.keyBy(r -> r.f0).max(1).print();
//        stream.keyBy(r -> r.f0).minBy(1).print();
//        stream.keyBy(r -> r.f0).maxBy(1).print();
        env.execute();
}}