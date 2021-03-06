package com.bit2017.mapreduce.topn;

import java.io.*;
import java.util.*;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;

import com.bit2017.mapreduce.topn.CountCitation.*;

public class CountCitation {
	public static class MyMapper extends Mapper<Text, Text, Text, LongWritable> {

		private Text word = new Text();
		LongWritable one = new LongWritable(1L);

		@Override
		protected void map(Text key, Text value, Mapper<Text, Text, Text, LongWritable>.Context context)
				throws IOException, InterruptedException {
			/* log.info("-------------> map() called"); */
			String line = value.toString();

			/* log.info("----------->tokenize worked"); */
			word.set(line);
			context.write(word, one);
		}
	}


	public static class MyReducer extends Reducer<Text, LongWritable, Text, Text> {

		private LongWritable sumWritable = new LongWritable();

		@Override
		protected void reduce(Text key, Iterable<LongWritable> values,
				Reducer<Text, LongWritable, Text, Text>.Context context)
				throws IOException, InterruptedException {

			/*
			 * long sum =0; for(LongWritable value : values) { sum +=
			 * value.get(); log.info("------------>" + sum); }
			 */

			long unique = 0;
			for (LongWritable value : values) {
				unique += value.get();
			}

			/* sumWritable.set(sum); */
			sumWritable.set(unique);
			// context.getCounter("Words Status", "Count of all
			// Words").increment(sum);
			context.getCounter("Words Status", "Count unique words").increment(unique);

			context.write(key, new Text(sumWritable.toString()));

		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();

		Job job = new Job(conf, "CountCitation");
		// 1. Job instance 초기화 과정
		job.setJarByClass(CountCitation.class);

		// 2. 맵퍼 클래스 지정
		job.setMapperClass(MyMapper.class);

		// 3. 리듀서 클래스 지정
		job.setReducerClass(MyReducer.class);

		// 4. 출력키 타입
		job.setMapOutputKeyClass(Text.class);

		// 5. 출력밸류 타입
		job.setMapOutputValueClass(LongWritable.class);

		// 6. 입력파일 포맷 지정(생략)
		job.setInputFormatClass(KeyValueTextInputFormat.class);

		// 7. 출력파일 포맷 지정(생략 가능)
		job.setOutputFormatClass(TextOutputFormat.class);

		// 8.입력파일 이름 지정
		FileInputFormat.addInputPath(job, new Path(args[0]));

		// 9.출력 디렉토리 지정gg
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		// 10. 실행
		if (job.waitForCompletion(true) == false) {
			return;
		}

		Configuration conf2 = new Configuration();
		Job job2 = new Job(conf2, "Top N");

		job2.setJarByClass(TopN.class);

		job2.setMapperClass(TopN.MyMapper.class);

		// 3. 리듀서 클래스 지정
		job2.setReducerClass(TopN.MyReducer.class);

		// 4. 출력키 타입
		job2.setMapOutputKeyClass(Text.class);

		// 5. 출력밸류 타입
		job2.setMapOutputValueClass(LongWritable.class);

		// 6. 입력파일 포맷 지정(생략)
		job2.setInputFormatClass(KeyValueTextInputFormat.class);

		// 7. 출력파일 포맷 지정(생략 가능)
		job2.setOutputFormatClass(TextOutputFormat.class);

		// 8.입력파일 이름 지정
		FileInputFormat.addInputPath(job2, new Path(args[1]));

		// 9.출력 디렉토리 지정gg
		FileOutputFormat.setOutputPath(job2, new Path(args[1] + "/topN"));
		
		job2.waitForCompletion(true);
	}
}
