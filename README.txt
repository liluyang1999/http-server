Assignment: HTTP

Instruction: 
    The program has been packaged as HTTP.jar which has been put at the top of 
the folder, and the source codes are in the "src" folder. The server is developed using JAVA in 
IDEA platform.
    Starting the server only needs to directly input the command "java -jar HTTP.jar" in cmd,
then the HttpServer and HttpsServer will be launched respectively. The HttpServer is set up at 
port 8081 and HttpsServer is at port 8082. 
    Using the command "curl http://localhost:8081/add/7/8"
			or
"curl.exe -XPOST --data "{'operation': 'divide', 'arguments':[4.0,3.0]}" http://localhost:8081"
can test the server, the same in HttpsServer replacing "http" with "https", then the response result 
will be shown.
    
    As for HttpsServer, the server.p12 is the keystore of the server in PKCS12 and the server.cer is 
the certificate exported by it, the same in client.p12 and client.cer. The client certificate has 
been imported into the server keystore becoming trusted.

    When using the GET method to request the HttpServer, the URL in the wrong form will lead to 
the error status code such as 404 Not Found in the wrong arithmetic method, 400 Bad Request 
in the bad syntax of arguments, and so on. Using the POST method, the tag name "operation" and 
"arguments" (not case sensitive) need to be correct, or the exception and error status code will be 
triggered during conversion to JSON. The SSL is introduced in HttpsServer, others are the same 
with HttpServer. The server keystore is set to be loaded in the current folder.



