package dataStreamApi;

import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author liuhuan
 * @date 2022-11-02
 */
public class TransKeyByTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment(); env.setParallelism(1);
        DataStreamSource<Event> stream = env.fromElements( new Event("Mary", "./home", 1000L),
                new Event("Bob", "./cart", 2000L)
        );
// 使用 Lambda 表达式
        KeyedStream<Event, String> keyedStream = stream.keyBy(e -> e.user);
// 使用匿名类实现 KeySelector
        KeyedStream<Event, String> keyedStream1 = stream.keyBy(new
                KeySelector<Event, String>() {
            @Override
            public String getKey(Event e) throws Exception { return e.user;
            } });
        env.execute(); }
}
