import java.net.*;				
import java.io.*;				
import java.util.*;				
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatServer {				
				
	public static void main(String[] args) {			
		try{		
			ServerSocket server = new ServerSocket(10001);	
			System.out.println("������ ��ٸ��ϴ�.");	
			
			
			HashMap hm = new HashMap(); //hm = key : Id, value : pw	
			HashSet hs = new HashSet(); //hs = ������ ���
			
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
			broadcast(id + " ���� �����Ͽ����ϴ�.");	
			System.out.println("������ ������� ���̵�� " + id + "�Դϴ�.");	
			synchronized(hm){	
				hm.put(this.id, pw);
			}	
			initFlag = true;	
		}catch(Exception ex){		
			System.out.println(ex);	
		}		
	} // ������			
	
	
	public void run(){			
		try{		
			String line = null;	
			//�޼����� ������ �����ϰ� ������ �� ������� ��� ���
			Object obj = hm.get(id); 
			PrintWriter pw = (PrintWriter)obj;
			
			while((line = br.readLine()) != null){		
				
				if(line.equals("/quit")) //����
				{
					pw.println(line);
					pw.flush();
					break;
				}
				else if(line.equals("/list")) //���� ����� ���
					list();
				else if(line.indexOf("/addspam") == 0) //������ ���
					addspam(line);
				else if(line.equals("/listspam")) //������ ��� ���
					listspam();
				else if(line.indexOf("/to ") == 0) //�ӼӸ�
					sendmsg(line);
				else //�޼��� ����
					broadcast(id + " : " + line);
			}		
		}catch(Exception ex){			
			System.out.println(ex);		
		}finally{			
			synchronized(hm){		
				hm.remove(id);	
			}		
			broadcast(id + " ���� ���� �����Ͽ����ϴ�.");		
			try{		
				if(sock != null)	
					sock.close();
			}catch(Exception ex){}		
		}			
	} // run				

	public void list() //���� ����� ��� (���� �����â)
	{
		String userid;
		Set<String> users= hm.keySet(); //���� ����� Id
		Iterator<String> it = users.iterator();
		
		int count = 1;
		Object obj = hm.get(id); //���� ����� ����� ��û�� ���
		PrintWriter pw = (PrintWriter)obj;
		
		pw.println("li#*���� ����� ����� ������ �����ϴ�*");
		while(it.hasNext())
		{		
			userid= it.next();
			pw.println("li#"+ count + ". " + userid);
			count++;
		}
		pw.println("li#��� "+ (count-1) + "�� �Դϴ�.");
		pw.flush();
	}
	
	public void addspam(String msg) //������ ���
	{
		int start = msg.indexOf(" ") +1;
		int end = msg.length();			
		if(start< end) //����� �ԷµǾ��ٸ�
		{			
			String spamword = msg.substring(start, end);
			hs.add(spamword); //���
		}
	}
	
	public void listspam() //������ ��� ��� (������ ���â)
	{
		String spamword;
		Iterator<String> it = hs.iterator();
		
		int count = 1;
		Object obj = hm.get(id); //������ ����� ��û�� ���
		PrintWriter pw = (PrintWriter)obj;
		
		pw.println("ls#*���� ��ϵ� ������� ������ �����ϴ�*");
		while(it.hasNext())
		{	
			spamword= it.next();
			pw.println("ls#"+ count + ". " + spamword);
			count++;
		}
		
		pw.println("ls#��� "+ (count-1) + "�� �Դϴ�.");
		pw.flush();
	}
	
	public boolean checkspam(String msg) //������ ���� ���� �Ǻ�
	{
		String spamword;
		Iterator<String> it = hs.iterator();
		
		Object obj = hm.get(id); 
		PrintWriter pw = (PrintWriter) obj;
		while(it.hasNext())
		{
			spamword= it.next();
			if(msg.indexOf(spamword)!=-1) //����� �ִٸ� ���
			{
				pw.println("br#�Է��� ���ڿ��� ������ ["+ spamword +"]�� ���ԵǾ� �������� �ʾҽ��ϴ�. �����Ͻʽÿ�.");
				pw.flush();
				return false;
			}
		}
		return true; //����� ������� ����
	}
	
	public void sendmsg(String msg) //�ӼӸ� (��ȭâ)
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
					pw.println("br#"+ id + " ���� ������ �ӼӸ��� �����̽��ϴ�. :" + msg2);	
					pw.flush();
				}
			} // if	
		}		
	} // sendmsg			

	public void broadcast(String msg) //��ȭâ ��� (��ȭâ)
	{			
		synchronized(hm){		
			Collection collection = hm.values();	
			Iterator iter = collection.iterator();	
			//����ð� ���
			Date date = new Date(System.currentTimeMillis());
			SimpleDateFormat format = new SimpleDateFormat("a hh�� mm�� ss��");
			String time = format.format(date);
			
			if(checkspam(msg)) //����� �����ϰ� ���� �ʴٸ�
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
