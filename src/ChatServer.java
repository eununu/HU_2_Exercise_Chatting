import java.net.*;				
import java.io.*;				
import java.util.*;				
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatServer {				
				
	public static void main(String[] args) {			
		try{		
			ServerSocket server = new ServerSocket(10001);	
			System.out.println("접속을 기다립니다.");	
			
			
			HashMap hm = new HashMap(); //hm = key : Id, value : pw	
			HashSet hs = new HashSet(); //hs = 금지어 목록
			
			while(true){	
				Socket sock = server.accept();
				ChatThread chatthread = new ChatThread(sock, hm, hs);
				chatthread.start();
			} // while	
		}catch(Exception e){	
			System.out.println(e);
		}	
	} // main		
}			
			
class ChatThread extends Thread{		 	
	private Socket sock;		
	private String id;
	private BufferedReader br;		
	private HashMap hm;	
	private HashSet hs;
	private boolean initFlag = false;
	
	public ChatThread(Socket sock, HashMap hm, HashSet hs){		
		this.sock = sock;	
		this.hm = hm;
		this.hs = hs;
		try{	
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));	
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));	
			id = br.readLine();	
			broadcast(id + " 님이 접속하였습니다.");	
			System.out.println("접속한 사용자의 아이디는 " + id + "입니다.");	
			synchronized(hm){	
				hm.put(this.id, pw);
			}	
			initFlag = true;	
		}catch(Exception ex){		
			System.out.println(ex);	
		}		
	} // 생성자			
	
	
	public void run(){			
		try{		
			String line = null;	
			//메세지가 금지어 포함하고 있으면 그 사람에게 경고 출력
			Object obj = hm.get(id); 
			PrintWriter pw = (PrintWriter)obj;
			
			while((line = br.readLine()) != null){		
				
				if(line.equals("/quit")) //종료
				{
					pw.println(line);
					pw.flush();
					break;
				}
				else if(line.equals("/list")) //현재 사용자 출력
					list();
				else if(line.indexOf("/addspam") == 0) //금지어 등록
					addspam(line);
				else if(line.equals("/listspam")) //금지어 목록 출력
					listspam();
				else if(line.indexOf("/to ") == 0) //귓속말
					sendmsg(line);
				else //메세지 전송
					broadcast(id + " : " + line);
			}		
		}catch(Exception ex){			
			System.out.println(ex);		
		}finally{			
			synchronized(hm){		
				hm.remove(id);	
			}		
			broadcast(id + " 님이 접속 종료하였습니다.");		
			try{		
				if(sock != null)	
					sock.close();
			}catch(Exception ex){}		
		}			
	} // run				

	public void list() //현재 사용자 출력 (현재 사용자창)
	{
		String userid;
		Set<String> users= hm.keySet(); //현재 사용자 Id
		Iterator<String> it = users.iterator();
		
		int count = 1;
		Object obj = hm.get(id); //현재 사용자 목록을 요청한 사람
		PrintWriter pw = (PrintWriter)obj;
		
		pw.println("li#*현재 사용자 목록은 다음과 같습니다*");
		while(it.hasNext())
		{		
			userid= it.next();
			pw.println("li#"+ count + ". " + userid);
			count++;
		}
		pw.println("li#모두 "+ (count-1) + "명 입니다.");
		pw.flush();
	}
	
	public void addspam(String msg) //금지어 등록
	{
		int start = msg.indexOf(" ") +1;
		int end = msg.length();			
		if(start< end) //금지어가 입력되었다면
		{			
			String spamword = msg.substring(start, end);
			hs.add(spamword); //등록
		}
	}
	
	public void listspam() //금지어 목록 출력 (금지어 목록창)
	{
		String spamword;
		Iterator<String> it = hs.iterator();
		
		int count = 1;
		Object obj = hm.get(id); //금지어 목록을 요청한 사람
		PrintWriter pw = (PrintWriter)obj;
		
		pw.println("ls#*현재 등록된 금지어는 다음과 같습니다*");
		while(it.hasNext())
		{	
			spamword= it.next();
			pw.println("ls#"+ count + ". " + spamword);
			count++;
		}
		
		pw.println("ls#모두 "+ (count-1) + "개 입니다.");
		pw.flush();
	}
	
	public boolean checkspam(String msg) //금지어 포함 여부 판별
	{
		String spamword;
		Iterator<String> it = hs.iterator();
		
		Object obj = hm.get(id); 
		PrintWriter pw = (PrintWriter) obj;
		while(it.hasNext())
		{
			spamword= it.next();
			if(msg.indexOf(spamword)!=-1) //금지어가 있다면 경고
			{
				pw.println("br#입력한 문자열에 금지어 ["+ spamword +"]가 포함되어 전송하지 않았습니다. 조심하십시오.");
				pw.flush();
				return false;
			}
		}
		return true; //금지어를 사용하지 않음
	}
	
	public void sendmsg(String msg) //귓속말 (대화창)
	{				
		int start = msg.indexOf(" ") +1;			
		int end = msg.indexOf(" ", start);			
		if(end != -1){			
			String to = msg.substring(start, end);		
			String msg2 = msg.substring(end+1);		
			Object obj = hm.get(to);	
			
			if(obj != null){		
				PrintWriter pw = (PrintWriter)obj;	
				if(checkspam(msg2))
				{
					pw.println("br#"+ id + " 님이 다음의 귓속말을 보내셨습니다. :" + msg2);	
					pw.flush();
				}
			} // if	
		}		
	} // sendmsg			

	public void broadcast(String msg) //대화창 출력 (대화창)
	{			
		synchronized(hm){		
			Collection collection = hm.values();	
			Iterator iter = collection.iterator();	
			//현재시간 출력
			Date date = new Date(System.currentTimeMillis());
			SimpleDateFormat format = new SimpleDateFormat("a hh시 mm분 ss초");
			String time = format.format(date);
			
			if(checkspam(msg)) //금지어를 포함하고 있지 않다면
			{
				while(iter.hasNext()){	
					PrintWriter pw = (PrintWriter)iter.next();
					pw.println("br#["+time+"] "+msg);
					pw.flush();
				}
			}
		}		
	} // broadcast			
}				
