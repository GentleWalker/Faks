package ket;
//paket ket
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class Node implements Watcher{

	 private int votes=0; 
	 ZooKeeper zookeeper; 
	 boolean isLeader = false; 
	 boolean isVoter = false; 
	 private int id; 
	 private int i=0; 
	 
	public Node(int number) { 
		super(); 
		this.id=number;	
	}
	
	void start() throws IOException, InterruptedException, KeeperException {				
        zookeeper = new ZooKeeper("localhost:2181", 5000, this); 					
    }

    void stopzookeeper() throws Exception {
        zookeeper.close();
    }

    public boolean checkIfLeaderExist() throws InterruptedException {			
        while(true) {															
            try {
                byte[] data = zookeeper.getData("/leader", true, null);			
                isLeader = data != null;										
                if(!isLeader) isVoter=true;										
                return true;													
            } catch (KeeperException.NoNodeException e) {						
                return false;
            } catch (KeeperException e) {										
                e.printStackTrace();
                return false;
            }
        }
    }
    
    public void runForLeader() throws InterruptedException, KeeperException {		
        if(!checkIfLeaderExist()) {													
            try {
	            zookeeper.create("/leader", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);		
	            isLeader = true;																			
            } catch (KeeperException.NodeExistsException e) {												
                isLeader = false;	
            } catch (KeeperException.ConnectionLossException e) {											
            	isLeader = false;
            	e.printStackTrace();
            }
        }else {
        	try {
	            zookeeper.create("/votes/vote-"+this.id, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL); 
	            isVoter = true;													
            } catch (KeeperException.NodeExistsException e) {					
                isVoter = false;
            } catch (KeeperException.ConnectionLossException e) {				
            	isVoter = false;
            	e.printStackTrace();
            }	
        }

    }

	public void register() throws InterruptedException, KeeperException {				
		runForLeader(); 
	}

	
	@Override
	public void process(WatchedEvent event) {											
		i++;
		if (event.getType() == Event.EventType.NodeDataChanged) {						
			try {
				byte[] data = zookeeper.getData("/leader", true, null);					
				String lead_to_str=new String(data);									
				if(lead_to_str.equals("start")) {										
				    if(this.isVoter) {
				    	try {
							zookeeper.setData("/votes/vote-"+this.id,generate_Vote().getBytes(), 0);		
						} catch (KeeperException | InterruptedException e) {
							e.printStackTrace();
						}
				    }
				}else if ((lead_to_str.equals("da") || lead_to_str.equals("ne"))&& i<4){		
					if(this.isVoter) {
						byte[] bytes=zookeeper.getData("/votes/vote-"+this.id, true, null);
						String byte_to_str=new String(bytes);
						if(byte_to_str.equals(lead_to_str))
							byte_to_str="Moj glas je ispravan";										
						else
							byte_to_str="Moj glas je bio pogresan";									
						zookeeper.setData("/votes/vote-"+id,byte_to_str.getBytes(), -1);
						System.out.println("Voter-"+this.id+": "+byte_to_str);
				    }else {
				    	
				    }
				}
			} catch (KeeperException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private String generate_Vote() { 
		Random rand=new Random();
		if(rand.nextBoolean()) {																	
			System.out.println("Voter-"+this.id+":da");
			return "da";
		}
		System.out.println("Voter-"+this.id+":ne");													
		return "ne";	
	}
	
	public void count_Votes() throws KeeperException, InterruptedException {	
		for(int i=2;i<5;i++) {
				byte[] s=this.zookeeper.getData("/votes/vote-"+i, true, null);					
				String str=new String(s);														
				if(str.equals("da")) {															
					votes++;
				}else {
					votes--;
				}		
		}
		if(votes>0) { 	
			this.zookeeper.setData("/leader", "da".getBytes(), -1);				
		}else {															
			this.zookeeper.setData("/leader", "ne".getBytes(), -1);							
		}
	}
	
	public static Node get_Leader(List<Node> nodes) {										
		for(int i=0;i<4;i++) {
			if(nodes.get(i).isLeader)
				return nodes.get(i);
		}
		return null;
	}

	public int getId() { 
		return id;
	}

}

public class Main {

	public static void main(String[] args) throws Exception {   
		Logger.getRootLogger().setLevel(Level.OFF);        
		List<Node> nodes= new ArrayList<>(); 
		
		for(int i=1;i<5;i++) { 
			nodes.add(new Node(i)); 
		}
		for(Node n: nodes) { 
			n.start(); 
			n.register();
		}
		
		Node.get_Leader(nodes).zookeeper.setData("/leader", "start".getBytes(), -1); 
		Thread.sleep(3000);	
		Node.get_Leader(nodes).count_Votes(); 
		Thread.sleep(120000);
	}

}
