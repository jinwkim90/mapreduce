package com.bit2017.mapreduce.index;

import java.io.*;
import java.net.*;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;

public class CreateESIndex {

	private static String baseURL = "";

	public static class ESIndexMapper extends Mapper<Text, Text, Text, Text> {

		@Override
		protected void setup(Mapper<Text, Text, Text, Text>.Context context) throws IOException, InterruptedException {
			String[] hosts = context.getConfiguration().getStrings("ESServer");
			baseURL = "http://" + hosts[0] + ":9200/wikipedia/doc/";
		}

		@Override
		protected void map(Text docId, Text contents, Mapper<Text, Text, Text, Text>.Context context)
				throws IOException, InterruptedException {

			// URL Connection 객체
			URL url = new URL(baseURL + docId);
			HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
			urlCon.setDoOutput(true);
			urlCon.setRequestMethod("PUT");

			// JSON 문자열 만들기
			String line = contents.toString().replace("\\", "\\\\").replace("\"", "\\\"");
			String json = "{\"body\":\"" + line + "\"}";

			// 데이터 보내기
			OutputStreamWriter out = new OutputStreamWriter(urlCon.getOutputStream());
			out.write(json);
			out.close();

			// 응답받기
			String lines = "";
			BufferedReader br = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
			while ((line = br.readLine()) != null) {
				lines += line;
			}

			if (lines.indexOf("\"successful\":1, \"failed\":0") < 0) {
				context.getCounter("Index Stats", "fail").increment(1);

			} else {
				context.getCounter("index Stats", "success").increment(1);
			}
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();

		Job job = new Job(conf, "CreateESIndex ");
		// 1. Job instance 초기화 과정
		job.setJarByClass(CreateESIndex.class);

		// 2. 맵퍼 클래스 지정
		job.setMapperClass(ESIndexMapper.class);

		// 3. 리듀서 클래스 지정
		/* job.setReducerClass(MyReducer.class); */

		// 리듀스 개수 지정
		job.setNumReduceTasks(0);

		// 4. 맵출력키 타입
		job.setMapOutputKeyClass(Text.class);

		// 5. 맵 출력밸류 타입
		job.setMapOutputValueClass(Text.class);

		// 6. 입력파일 포맷 지정(생략)
		job.setInputFormatClass(KeyValueTextInputFormat.class);

		// 8.입력파일 이름 지정
		FileInputFormat.addInputPath(job, new Path(args[0]));

		// 9.출력 디렉토리 지정
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		// ES Server 지정
		job.getConfiguration().setStrings("ESServer", args[2]);
		// 10. 실행
		job.waitForCompletion(true);

	}
}
