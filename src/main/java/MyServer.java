import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedInputStream;
import java.io.*;

public class MyServer {

  public MyServer() {

  }
  public static void main(String args[]){
    ServerSocket serverSocket= null;
    Socket socket =null;
    BufferedReader datainputstream=null;

    try{
      serverSocket = new ServerSocket(3334);
      System.out.println("Listening ...");
    }catch(IOException e){
      e.printStackTrace();
    }
    try{
      socket = serverSocket.accept();

    }catch(IOException e){
      System.out.println("can't listen given port \n");
      System.exit(-1);
    }
    try{
      datainputstream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      System.out.println("ip:"+socket.getInetAddress());
    }catch(IOException e){
      System.out.println("can't read File \n");
      System.exit(-1);
    }

    try{


      StringBuilder sb = new StringBuilder();
      String line = null;
      while((line=datainputstream.readLine())!=null){
        sb.append(line+"\n");
        System.out.println("actualMessage:"+line);
      }
      System.out.println("message:"+sb.toString());
      datainputstream.close();


    }catch(IOException e){

      System.out.println("Cant read file \n");
    }finally{
      if(socket!=null){
        try{
          socket.close();
        }catch(IOException e){
          e.printStackTrace();
        }
      }
      if(datainputstream!=null){
        try{
          datainputstream.close();
        }catch(IOException e){
          e.printStackTrace();
        }
      }

    }

  }



}