package edu.mit.csail.cgs.deepseq.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ComparePWM {
	
	public static void main(String[] args) {
//		parseSTAMP(args);		// JTUX.motifs top_encode_PFM.txt out_match_pairs.txt
		parseENCODETest(args);	// C:\Data\ENCODE\MotifCompare\info\known-match-ranks-merged_sorted.txt  C:\Data\ENCODE\MotifCompare\info\ENCODE_name_mapping_Pouya.txt C:\Data\ENCODE\MotifCompare\encode_PFM_2_mapping.txt Yuchun-run2
	}
	/**
	 * To compare GEM's motif results with those from other methods on ENCODE ChIP-Seq data<br>
	 */
	private static void parseENCODETest(String[] args){
		HashMap<String, String> map = new HashMap<String, String>();	// ENCODE expt name --> Factor group
		try {	
			BufferedReader bin = new BufferedReader(new InputStreamReader(new FileInputStream(args[1])));
	        String line;
	        bin.readLine();		// header line
	        bin.readLine();
	        while((line = bin.readLine()) != null) { 
	        	line = line.trim();
	        	String[] f = line.split("\t");
	        	map.put(f[1].trim(), f[0].trim());
	        }
	        if (bin != null) {
	            bin.close();
	        }
        } catch (IOException e) {
        	System.err.println("Error when processing "+args[1]);
            e.printStackTrace(System.err);
        }
        
        HashMap<String, String> encodeOverlaps = new HashMap<String, String>();	// ENCODE expt name --> my PFM results
		try {	
			BufferedReader bin = new BufferedReader(new InputStreamReader(new FileInputStream(args[2])));
	        String line;
	        bin.readLine();		// header line
	        bin.readLine();
	        while((line = bin.readLine()) != null) { 
	        	line = line.trim();
	        	String[] f = line.split("\t");
	        	String f0 = f[0].trim();
	        	if (!encodeOverlaps.containsKey(f[1].trim()))
	        			encodeOverlaps.put(f[1].trim(), f0.substring(0, f0.lastIndexOf("_2_")));
	        }
	        if (bin != null) {
	            bin.close();
	        }
        } catch (IOException e) {
        	System.err.println("Error when processing "+args[2]);
            e.printStackTrace(System.err);
        }
        
        ArrayList<String> others = new ArrayList<String>();
        others.add("MEME");
        others.add("AlignACE");
        others.add("MDscan");
        others.add("Trawler");

        StringBuilder winSB = new StringBuilder();
        StringBuilder loseSB = new StringBuilder();
        StringBuilder tieSB = new StringBuilder();
        String myMethod = args[3];
		try {	
			BufferedReader bin = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
	        String line;
	        HashMap<String, Integer> method2hitRank = new HashMap<String, Integer>();	// method name --> hit rank
	        String prevExpt = "";
	        while((line = bin.readLine()) != null) { 
	        	line = line.trim();
	        	String[] f = line.split("\t");
	        	String name = f[0].trim().replaceFirst("_r1", "");
	        	if (!encodeOverlaps.containsKey(name))						// only look at expts mapped to my PFM results
	        		continue;
	        	if (prevExpt.equals(""))
	        		prevExpt = name;
	        	int rank=-1;
	        	if (f.length==4 && !f[3].trim().equals(""))
	        		rank = Integer.parseInt(f[3].trim());
	        	if (name.equals(prevExpt)){
	        		if (rank!=-1)
	        			method2hitRank.put(f[1], rank);
	        	}
	        	else{
        			// have collected all of this expt
        			int myRank = 100;
        			if (method2hitRank.containsKey(myMethod))
        				myRank = method2hitRank.get(myMethod);
        			for(String m:others){
        				if (!method2hitRank.containsKey(m))
        					continue;
        				int or = method2hitRank.get(m);
        				if (myRank<or)
        					winSB.append(map.get(prevExpt)+"\t"+prevExpt+"\t"+myRank+"\t"+m+"\t"+or+"\t"+encodeOverlaps.get(prevExpt)+"\n");
        				else if (myRank>or)
        					loseSB.append(map.get(prevExpt)+"\t"+prevExpt+"\t"+myRank+"\t"+m+"\t"+or+"\t"+encodeOverlaps.get(prevExpt)+"\n");
        				else
        					tieSB.append(map.get(prevExpt)+"\t"+prevExpt+"\t"+myRank+"\t"+m+"\t"+or+"\t"+encodeOverlaps.get(prevExpt)+"\n");
        			}
        			// start with a new expt
	        		prevExpt = name;
	        		method2hitRank.clear();
		        	rank=-1;
		        	if (f.length==4 && !f[3].trim().equals(""))
		        		rank = Integer.parseInt(f[3].trim());
	        		if (rank!=-1)
	        			method2hitRank.put(f[1], rank);
	        	}
	        }
	        // process the last expt
	        if (!prevExpt.equals("")){		// have collected all of this expt
    			int myRank = 100;
    			if (method2hitRank.containsKey(myMethod))
    				myRank = method2hitRank.get(myMethod);
    			for(String m:others){
    				if (!method2hitRank.containsKey(m))
    					continue;
    				int or = method2hitRank.get(m);
    				if (myRank<or)
    					winSB.append(prevExpt+"\t"+myRank+"\t"+m+"\t"+or);
    				else if (myRank>or)
    					loseSB.append(prevExpt+"\t"+myRank+"\t"+m+"\t"+or);
    				else
    					tieSB.append(prevExpt+"\t"+myRank+"\t"+m+"\t"+or);
    			}
    		}
	        System.out.println("\n************** Lose **************\n"+loseSB);
	        System.out.println("\n************** Win **************\n"+winSB);
	        System.out.println("\n************** Tie **************\n"+tieSB);
	        if (bin != null) {
	            bin.close();
	        }
        } catch (IOException e) {
        	System.err.println("Error when processing "+args[0]);
            e.printStackTrace(System.err);
        }
        
	}
	
	/** 
	 * To compare GEM motif result on ENCODE data with known motif in database using STAMP
	 */
	private static void parseSTAMP(String[] args){
		try {	
			HashSet<String> mismatch = new HashSet<String>(){{
			    add("P");
			    add("R");
			    add("Su");
			    add("AP");
			    add("E2");
			    add("ER");
			    add("C1");
			    add("FOX");
			    add("ERR");
			    add("RSRFC4");
			    add("h");
			    add("T");
			    add("D");
			    add("z");
			    add("gt");
			    add("C15");
			    add("br");
			    add("CF1");
			}};
			TreeSet<String> dbs = new TreeSet<String>();
			BufferedReader bin = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
	        String line;
			while((line = bin.readLine()) != null) { 
		        line = line.trim();
	        	if (line.startsWith("DE")){
	        		String name = line.substring(3, line.length());
	        		String[] f = name.split("_");
	        		String tf_db = f[1];
	            	if (f[0].equals("J"))
	            		tf_db = f[2];
	        		dbs.add(tf_db);
	        	}
			}
			TreeSet<String> tfsPWM = new TreeSet<String>();
	        TreeSet<String> exptsPWM = new TreeSet<String>();
	        TreeMap<String, String> tfsInDB = new TreeMap<String, String>();
	        TreeSet<String> exptsInDB = new TreeSet<String>();
			bin = new BufferedReader(new InputStreamReader(new FileInputStream(args[1])));
			while((line = bin.readLine()) != null) { 
		        line = line.trim();
	        	if (line.startsWith("DE")){
	        		String name = line.substring(3, line.length());
	        		String[] f = name.split("_");
	        		String tf = f[0];
		            String cell = f[1];
		            String pi = f[2];
		            tfsPWM.add(tf);
		            exptsPWM.add(tf+"_"+cell+"_"+pi);
		            for (String db:dbs){
			            if ((tf.toLowerCase().contains(db.toLowerCase())||db.toLowerCase().contains(tf.toLowerCase()))
			            		&& !mismatch.contains(db)){
			            	tfsInDB.put(tf, db);
			            	exptsInDB.add(tf+"_"+cell+"_"+pi);
			            	break;
		            	}
		            }
	        	}
			}
	        System.out.println("*********************");
	        System.out.println("TF PWM found: "+tfsPWM.size());
	        System.out.println("\n*********************");
	        System.out.println("Expt PWM found: "+exptsPWM.size());
	        
	        System.out.println("\n*********************");
	        System.out.println("Known TF: "+tfsInDB.size());
	        for (String tf:tfsInDB.keySet()){
	        	System.out.println(tf+"\t"+tfsInDB.get(tf));
	        }
	        System.out.println("\n*********************");
	        System.out.println("Expt with known TF: "+exptsInDB.size());
//	        for (String e:exptsInDB){
//	        	System.out.println(e);
//	        }
			
			bin = new BufferedReader(new InputStreamReader(new FileInputStream(args[2])));
			
	        StringBuilder sb = new StringBuilder();
	        TreeSet<String> tfs = new TreeSet<String>();
	        TreeSet<String> expts = new TreeSet<String>();
	        while((line = bin.readLine()) != null) { 
		        line = line.trim();
	        	if (line.startsWith(">")){
	        		String name = line.substring(2, line.length());
		            String[] f = name.split("_");
		            String tf = f[0];
		            String cell = f[1];
		            String pi = f[2];
		            int id = Integer.parseInt(f[4]);
		            for (int i=0;i<10;i++){
		            	line = bin.readLine();
		            	f = line.split("\t");
		            	String evalue = f[1];
		            	String match = f[0];
		            	f = match.split("_");
		            	String tf_db = f[1];
		            	if (f[0].equals("J"))
		            		tf_db = f[2];
		            	if (tf.toLowerCase().contains(tf_db.toLowerCase())||tf_db.toLowerCase().contains(tf.toLowerCase())){
		            		sb.append(String.format("%s\t%d\t%s\t%d", tf, id, match, i));
		            		if (Double.parseDouble(evalue)>1e-6)
		            			sb.append("\t"+evalue);
		            		sb.append("\n");
		            		tfs.add(tf);
		            		expts.add(tf+"_"+cell+"_"+pi);
		            	}
		            }
	        	}
	        }

	        System.out.println("\n*********************");
	        System.out.println("Match TF: "+tfs.size()+"\t"+tfs.size()*1.0/tfsInDB.size());
	        for (String t:tfs){
	        	System.out.println(t);
	        }
	        Set<String> tfs_not_match = tfsInDB.keySet();
	        tfs_not_match.removeAll(tfs);
	        System.out.println("\n*********************");
	        System.out.println("unMatch TF: "+tfs_not_match.size());
	        for (String t:tfs_not_match){
	        	System.out.println(t);
	        }
	        System.out.println("\n*********************");
	        System.out.println("Match Expt: "+expts.size()+"\t"+expts.size()*1.0/exptsInDB.size());
//	        for (String e:expts){
//	        	System.out.println(e);
//	        }
	        System.out.println("\n*********************");
	        System.out.println(sb.toString());
	        
	        if (bin != null) {
	            bin.close();
	        }
        } catch (IOException e) {
        	System.err.println("Error when processing "+args[0]);
            e.printStackTrace(System.err);
        }
	}

}
