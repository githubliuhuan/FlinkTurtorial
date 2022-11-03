package com.atguigu.wc;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.AggregateOperator;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.operators.FlatMapOperator;
import org.apache.flink.api.java.operators.UnsortedGrouping;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

/**
 * @author liuhuan
 * @date 2022-10-31
 */
public class BatchWordCount {
    public static void main(String[] args) throws Exception {
        // 1.创建执行环境
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // 2.从文件读取数据 按行读取
        DataSource<String> lineDs = env.readTextFile("input/word.txt");

        // 3.转换数据格式
        FlatMapOperator<String, Tuple2<String, Long>> wordAndOneTuple = lineDs.flatMap((String line, Collector<Tuple2<String, Long>> out) -> {
                    String[] words = line.split(" ");
                    for (String word : words) {
                        out.collect(Tuple2.of(word, 1L));
                    }

                }).returns(Types.TUPLE(Types.STRING, Types.LONG));

        // 按照word进行分组
        UnsortedGrouping<Tuple2<String, Long>> wordAndOneGroup = wordAndOneTuple.groupBy(0);

        // 分组内进行聚合
        AggregateOperator<Tuple2<String, Long>> sum = wordAndOneGroup.sum(1);

        // 打印输出
        sum.print();
    }

}
