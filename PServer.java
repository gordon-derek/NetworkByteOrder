/*
 *   Authors: Derek Gordon
 *   Class: CIS410 - Networking
 *   Date: February 22, 2013
 *   Project: Assignment 4 - Binary RPN Server
 *
 *   Purpose: This server takes input in NBO postfix
 *   notation and evaluates the equation.  
 *
 *   If the equation is not complete or valid, an error
 *   is returned.
 */

import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.*;

/* multi-threaded RPN Calc server */

public class PServer extends Thread {

    // number of client threads started
    public static int nclient = 0;
    // default server port
    public static final int defaultServerPort = 5099;

    // server thread uses this socket
    public Socket s = null;

    public static boolean invalid = false;

    // server thread:
    // create a server that uses this socket endpoint
    public PServer(Socket s) {
	this.s = s;
    }

    // start a service dispatcher on the server port
    // and create server threads for each incoming connection
    public static void startService(int serverPort) throws Exception {

	ServerSocket svr = new ServerSocket(serverPort);

	while(true) {
	    Socket s = svr.accept();
	    new PServer(s).start();
	}
    }

    // server thread code:
    // this is executed in a separate thread for each incoming connection
    // allowing for multiple simultaneous server threads
    public void run() {
	nclient++;
        System.out.println("Starting PServer thread #" + nclient);
	System.setProperty("line.terminator", "\r\n");
      
	try{
	    //establishes data input from client
	    InputStream in = s.getInputStream();
	    //establishes data output to client
	    OutputStream out = s.getOutputStream();
	    //holds numbers in operation
	    Stack<Integer> stack = new Stack<Integer>();
	    
	    //holds tag bytes
	    int n = in.read();
	    while(n != 255){
		try{
		    while(n != 0 && !invalid){
			//determines what operation to be performed from tag byte
			switch(n){
			case 1: //convert from nbo and push on stack
			    byte[] input = new byte[4];
			    in.read(input);
			    stack.push(new NBO().nbo2int(input));
			    break;
			case 2: evaluate(stack, out, '+'); break; 
			case 3: evaluate(stack, out, '-'); break;
			case 4: evaluate(stack, out, '*'); break;
			case 5: evaluate(stack, out, '/'); break;
			case 6: evaluate(stack, out, '%'); break;
			case 7: evaluate(stack, out, '!'); break;
			default: out.write(1);
			    invalid = true; break;
			}//end switch
			n = in.read();
		    }//end while
		    
		    //equation was tagged invalid in process of evaluating
		    if(!invalid){
			if(!stack.isEmpty()){
			    int result = stack.pop();
			    if(!stack.isEmpty())
			    out.write(3);
			    else{
				out.write(0);
				byte[] returnB = new byte[4];
				new NBO().int2nbo(result, returnB);
				out.write(returnB);
			    }//end else
			}//end if
			else{
			    out.write(2);
			}
		    }//end if
		    
		    while(n != 0){
			n = in.read();
		    }
		    
		    invalid = false;

		    //push output to client
		    out.flush();
		    //get next tag byte
		    n = in.read();
		}catch(Exception e){
		    out.write(1);
		}
		}//end while
	   
	}catch(IOException e){}
        
    }
	
    //Pre: test contains a character that could be an operator
    //Post: true is returned if test is an operator, false otherwise
    //Purpose: To test the current inputted character to see if it is an operator
    public static boolean isOperator(char test){
	switch(test){
	case '*':
	case '+':
	case '-':
	case '/': 
	case '%':
	case '!': return true;
	default: return false;
	}
    }

    //Pre: numbers have been read in and pushed onto the stack, an operator has been read
    //Post: an single part of an equation is solved, unless the mathematical laws aren't met
    //Purpose: to take in two integers off the stack and solve using the operator given
    public static void evaluate(Stack<Integer> stack, OutputStream out, char operator ){
	try{
	    //stack is empty cannot solve
	    if(stack.isEmpty()){
		invalid = true;
		out.write(2);
	    }//end if
	    else{//stack has atleast one object
		//the integer on top of stack is the second operand
		int op2 = stack.pop();
		
		//bang operator signifies negation
		if(operator == '!')
		    stack.push(0 - op2);
		//negation not being used so need another operand
		else if(stack.isEmpty()){
		    invalid = true;
		    out.write(2);
		}//end else if
		else{
		    //two operands present... solve
		    int op1 = stack.pop();
		    
		    //cannot divide/mod by 0
		    if((operator == '/' || operator == '%') && op2 == 0){
			invalid = true;
			out.write(4);
		    }//end if
		    else{
			//all is well, solve operation
			stack.push(solve(op1, op2, operator));
		    }//end else
		}//end else
	    }//end else
	}//end try
	catch(IOException e){invalid = true; System.out.println("IO Exception");}
    
    }

    //Pre: op1 and op2 are the corrosponding integers in equation
    //Post: the equation is solved in form op1 operator op2 and returned
    //Purpose: to solve the piece of the full equation and return
    public static int solve(int op1, int op2, char operator){
	switch(operator){
	case '*': return (op1 * op2);
	case '+': return (op1 + op2);
	case '-': return (op1 - op2);
	case '/': return (op1 / op2);
	case '%': return (op1 % op2);
	default: return 0;
	}
    }

    //Pre: test contains a character that could be an operand
    //Post: true is returned if test is an operand, false otherwise
    //Purpose: To test the current inputted character to see if it is an operand    
    public static boolean isInt(char test){
	switch(test){
	case '0':
	case '1':
	case '2':
	case '3':
	case '4':
	case '5':
	case '6':
	case '7':
	case '8':
	case '9': return true;
	default: return false;
	}
    }

    // create an echo server dispatcher on the specified server port
    // (defaults to defaultServerPort)
    public static void main(String [] args) {
	PServer server;
	int nargs = args.length;
	int serverPort = defaultServerPort;
	if (nargs == 1)
	    serverPort = Integer.parseInt(args[0]);
      
	try {
	    startService(serverPort);
	} catch (Exception e) {
	    System.err.println(e);
	    System.exit(1);
	}
    }

}
