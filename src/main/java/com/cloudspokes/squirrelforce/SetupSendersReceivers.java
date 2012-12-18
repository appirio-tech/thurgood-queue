package com.cloudspokes.squirrelforce;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@SuppressWarnings("serial")
public class SetupSendersReceivers extends HttpServlet {
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
    }
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		System.out.println("In MessageProcessor init()");
		String initLangs = config.getInitParameter("langs");
		String[] langs = initLangs.split(";");
		
		//Call helper method to setup all senders and receivers
		setupSenderReceiverListeners(langs);
	}
	
	private void setupSenderReceiverListeners(String[] langs) {
		String uri = System.getenv("CLOUDAMQP_URL");

		if (uri == null)
			uri = "amqp://guest:guest@localhost";

		//Get connection and start receivers and senders
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUri(uri);
	
			Connection connection = factory.newConnection();
			
			//Setup main sender/receiver - this will receive from client and push out to appropriate receivers
			MainReceiverSender mainReceiverSender = new MainReceiverSender(connection);
			Thread t = new Thread(mainReceiverSender);
			t.start();
			
			//For each of the langs setup, setup LangReceiver
			for (String lang : langs) {
				LangReceiver recv = new LangReceiver(lang, connection);
				Thread t1 = new Thread(recv);
				t1.start();
			}
		} catch (Exception e) {
			System.out.println("******************* Exception in SetupSendersReceivers *******************");
			e.printStackTrace();
		}
	}

}

