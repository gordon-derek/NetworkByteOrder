/*
 *   Authors: Derek Gordon
 *   Class: CIS410 - Networking
 *   Date: March 12, 2013
 *   Project: Assignment 4 - Binary RPN Server
 *
 *   Purpose: This client take in lines of
 *     infix equations, converts them to postfix(Reverse Polish),
 *     then Network Byte Order and sends them to a server to get evaluated.
 *
 *    Prints output from the socket to screen for user.
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;

class PClient{

    //Pre: a character has been recieved
    //Post: the appropriate tag byte is returned or -1 for not an operator
    //Purpose: to link the operator with its appropriate tag byte or
    //   return -1 showing it is not an operator
    public static int isOperator(char test){
	    switch(test){
	    case '+': return 2;
	    case '-': return 3;
	    case '*': return 4;
	    case '/': return 5;
	    case '%': return 6;
	    case '!': return 7;
	    default: return -1;
	    }//end switch
    }

    //Pre: a suspected integer has been taken from input
    //Post: the string has been converted to an integer and sent to server
    //Purpose: to conver the string to an integer and send to server if valid,
    // if not a valid integer then return false and print an error message
    public static boolean sendInt(String op, OutputStream outToServer){
		try{
		    //parse integer from string
		    int input = Integer.parseInt(op);
		    //holds the conversion of the integer to bytes
		    byte [] output = new byte[4];
		    new NBO().int2nbo(input, output);
		    try{
			//sends to server
			outToServer.write(1);//tag byte
			outToServer.write(output);//entire 4-byte integer
			return true;
		    }//end try
		    catch(IOException e){return false;}//end catch
		    
		}//end try
		catch(NumberFormatException e){
		    System.err.println("Incorrect Input. Please try again");
		    return false;
		}//end catch
    }

    //returns if the current character is a space
    public static boolean isSpace(char test){
		return test == ' ';
    }
    
    public static void main(String args[])throws Exception{
	    System.out.println("Insert infix Equations, Single '.' terminates program");
	  
	    String server = args[0];  //server address
	    String sentence;          //line of input
	    String modifiedSentence;  //line of input after conversion to postfix

	try{	
	    //Scanner to read input from terminal
	    Scanner eIn = new Scanner(new InputStreamReader(System.in));
	    //connects to server on specified port number
	    Socket clientSocket = new Socket(server, 5099);
	    //establishes data output to server
	    OutputStream outToServer = clientSocket.getOutputStream();
	    //establishes data input from server
	    InputStream inFromServer = clientSocket.getInputStream();
	    
	    //get first line of input
	    while(eIn.hasNextLine()){
			sentence = eIn.nextLine();
			if(sentence.equals("."))
		    break;
			try{
			    //convert to postfix
			    sentence = new I2P(sentence).parse();
			}
			catch(ParseException e){}
			//System.out.println("Makes it Here");
		    //put in Network Byte Order
		    String op = "";
		    boolean valid = true;
		    for(int i = 0; i < sentence.length() && valid; i++){
			//get current character in sentence
			char test = sentence.charAt(i);
			if(isSpace(test)){
			    if(op.length() > 1) //operator only has length 1
				valid = sendInt(op, outToServer);
			    
			    //could be operator or int
			    else if(op.length() == 1){
				//get only character
				test = op.charAt(0);
				//test for operator
				int operator = isOperator(test);
				if(operator == -1)	
				    valid = sendInt(op, outToServer);
				else
				    outToServer.write(operator);
			    }//end else if
			    else{
			    System.err.println("Illegal Input");
			    valid = false;
			    }//end else
			    op = "";
			}//end if
			else
			    op += sentence.charAt(i);
			
		    }//end for
		    //send tag byte showing end of expression
		    outToServer.write(0);
		    //push buffer to server
		    outToServer.flush();
		
		    //get result back from server, output
		    int n = inFromServer.read();
		    System.out.print("From Server: ");
		    switch(n){
			    case 0: byte[] result = new byte[4];
				inFromServer.read(result);
				System.out.println(new NBO().nbo2int(result)); break;
			    case 1: System.err.println("Illegal input"); break;
			    case 2: System.err.println("Too Few Operands"); break;
			    case 3: System.err.println("Too Many Operands"); break;
			    case 4: System.err.println("Attempt to Divide by Zero"); break;
		    default:break;
		    }//end switch
	    }//end while
	    
	    //close communication with server
	    outToServer.write(255);
	    outToServer.flush();
	}catch(UnknownHostException e){
	    System.out.println("Failed to Connect to Server(or you're breaking rules)");
	}
    }
}
