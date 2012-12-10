# Squirrelforce

## Setup Instructions

1. Create new Heroku application using embedded jetty servlet template. Note the project id. 
Note the project id. In this doc, using id: shashiapp
2. Add CloudAMQP Little Lemur addon to the application. 
3. Get APP environment variable CLOUDAMQP_URL in the new application's environment. 
4. Edit Constants.java file to change value of String constant CLOUD_AMQP_URL to the value got from step 3 above. 
This is required to run the client locally and send to RabbitMQ on Server.
5. Deploy to Heroku 

Note: For running Java server and client below, make sure you have the right classpath configured 
or use the classpath on command line. Here is the OSX invocation command below for starting the client. 

	cd /Users/Jeff/Documents/workspaces/cloudspokes/squirrelforce/target
	java -cp /Users/Jeff/Documents/java/rabbitmq-client.jar:./squirrelforce-1.0-SNAPSHOT.jar com.cloudspokes.squirrelforce.SenderClient local

## Running Client - Local

1. Start Main class. Web app will be started on http://localhost:8080
2. Start client using command 'java SenderClient local' 
3. Paste data from the file testData.dat to the command line
4. You will see data being sent in the client window and data being received and sent to Type queues in the server (i.e. Main) window.
5. On a browser go to http://localhost:8080. You will see statistics of the messages being sent by client and processed by server. 

## Running Client - Heroku

1. Start to view logs on command line: heroku logs --app <yourapp> --tail
2. Start client using command 'java SenderClient heroku'
3. Paste data from the file testData.dat to the command line
4. You will see data being sent in the client window and data being received and sent to Type queues in the server window, i.e. Heroku Logs window.
5. On a browser go to http://<yourapp>.herokuapp.com. You will see statistics of the messages being sent by client and processed by server. 

## Ruby Client

	require "bunny"
	b = Bunny.new ENV['CLOUDAMQP_URL'] # omit URL to run locally
	b.start
	q = b.queue("mainQueue")
	q.publish('{"url":"https://google.com","Type":"apex","Name":"CPS-1234","ID":"a0GU0000007AGDa"}')
	b.stop


## Test Data

{"url":"https://google.com","Type":"apex","Name":"CPS-1234","ID":"a0GU0000007AGDa"}
{"url":"https://google.com","Type":"ruby","Name":"CPS-123455","ID":"a0GU00ASD007AGDa"}
{"url":"https://google.com","Type":"java","Name":"CPS-12323424","ID":"a0GU0000007AGDa"}
{"url":"https://google.com","Type":"apex","Name":"CPS-1256734","ID":"a0GU0XCV07AGDa"}
{"url":"https://google.com","Type":"java","Name":"CPS-12323454","ID":"a0GU0000007AGDa"}
{"url":"https://google.com","Type":"javascript","Name":"CPS-1123213234","ID":"a0GU0000007AGDa"}
{"url":"https://google.com","Type":"python","Name":"CPS-123534534","ID":"a0GU0000007AGDa"}
{"url":"https://google.com","Type":"ruby","Name":"CPS-123353454","ID":"a0GU0AS07AGDa"}
{"url":"https://google.com","Type":"python","Name":"CPS-135345234","ID":"a0GU02143007AGDa"}
{"url":"https://google.com","Type":"ruby","Name":"CPS-1123123234","ID":"a0GU0000007AGDa"}
