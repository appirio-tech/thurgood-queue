package com.cloudspokes.squirrelforce;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class SenderClient {
	public static void main(String[] argv) throws Exception {
		if (argv.length != 1
				|| !(argv[0].equals("local") || argv[0].equals("heroku"))) {
			throw new IllegalArgumentException(
					"usage: java SenderClient [local|remote]");
		} 
		String env = argv[0];
		String uri = null;
		if (env.equals("local")) {
			uri = "amqp://guest:guest@localhost";
		} else {
			uri = Constants.CLOUD_AMQP_URL;
		}
		System.out.println("Environment: " + env);
		System.out.println("AMQP URL being used:" + uri);
		System.out.println("--------------------------------------");
		System.out.println("Enter json message to send to Main Queue");
		System.out.println("Enter x to exit");
		System.out.print("> ");
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUri(uri);
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line =  br.readLine();
		while (line != null) {
			if (line.equalsIgnoreCase("x")) {
				break;
			}
			channel.queueDeclare(Constants.QUEUE_NAME, false, false, false, null);
	
			channel.basicPublish("", Constants.QUEUE_NAME, null, line.getBytes());
			System.out.println(" [x] Sent '" + line + "'");
			System.out.println("");
			line =  br.readLine();
		}
		System.out.println("\n\nSquirrelforce!!!!!! Exiting now.");

		channel.close();
		connection.close();
	}
}
