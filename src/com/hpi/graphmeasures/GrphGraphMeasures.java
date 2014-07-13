package com.hpi.graphmeasures;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.carrotsearch.hppc.cursors.IntCursor;
import com.hpi.utils.MemoryUtils;

import toools.set.IntSet;
import grph.Grph;
import grph.algo.distance.PageRank;
import grph.algo.distance.MinimumEccentricityGraphCenter;
import grph.in_memory.InMemoryGrph;

public class GrphGraphMeasures

{
	private File input;
	private static final Logger L = Logger.getLogger(GrphGraphMeasures.class.getSimpleName());

	private Map<String, Integer> resources;
	private Map<Integer, String> resourceIds;
	private int lastResourceId;
	
	public static void main(String[] args) throws Exception
	{
		File f = new File(args[0]);
		GrphGraphMeasures test = new GrphGraphMeasures(f);
		test.compute(args[1]);
		return;
	}
	
	public GrphGraphMeasures(File input) {
		this.input = input;
		this.resources = new HashMap<String, Integer>();
		this.resourceIds = new HashMap<Integer, String>();
		this.lastResourceId = 0;
	}
	
	public void compute(String graphType) throws IOException {

		L.info("Load input graph.");
		MemoryUtils.printUsedMemory();
		Grph graph=new InMemoryGrph();
		FileInputStream fis = new FileInputStream(this.input);
			BufferedReader data = new BufferedReader(new InputStreamReader(fis));
			load_data(data, graph);
        if("pagerank".equalsIgnoreCase(graphType))
        {
        	L.info("Start computation.");
            Random r=new Random();
        	PageRank ranker = new PageRank(graph, r);	
        	ranker.compute();
        	L.info("Computation done.");
        	L.info("Writing results to file .");
        	writePageRankResultsToFile(graph,ranker,graphType);
        	L.info("Completed");
        }
        else if("Eccentricity".equalsIgnoreCase(graphType))
        {
        	L.info("Start computation.");
			MinimumEccentricityGraphCenter megc=new MinimumEccentricityGraphCenter();
            megc.compute(graph);
        	L.info("Computation done.");
        	L.info("Writing results to file .");
        	writeEccentricityResultsToFile(graph,graphType);
        	L.info("Completed");
        }
        else
        {
        	System.out.println("Please provide correct graph measure");
        	System.out.println("Usage <<inputturtleFile>> <<Pagerank or Eccentricity>>");
        }

		MemoryUtils.printUsedMemory();
		System.exit(0);

	}
	private void load_data(BufferedReader in,
			Grph graph) throws IOException {
		
		int edgeCnt = 0;
		String line;
		HashSet<String> seen = new HashSet<String>();

		while ((line = in.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line);
			String source = null;
			String destination = null;

			if (st.hasMoreTokens()) {
				source = st.nextToken().replace("<http://dbpedia.org/resource/", "").replace(">", "");
			}

			if (st.hasMoreTokens()) {
				// ignoring second node(predicate)
				st.nextToken();
				if (st.hasMoreTokens()) {
					destination = st.nextToken().replace("<http://dbpedia.org/resource/", "").replace(">", "");
					if (!destination.equals(source)) { // no self-references
						// no duplicate links
						if (!seen.contains(source + "---" + destination)) {
							graph.addDirectedSimpleEdge(getIntForResource(source), new Integer(edgeCnt++), getIntForResource(destination));
						} else {
							L.info("Dup " + source + " --- " + destination);
						}
					}
					if (st.hasMoreTokens())
						st.nextToken();// ignoring .
				} else {
					L.warning("3rd argument missing");
				}
			}
		}
		in.close();

		L.info("Last line: " + line);

		L.info("Edge count: " + edgeCnt + " // " + graph.getNumberOfEdges());
		L.info("Vertex count: // " + graph.getNumberOfVertices());
	}
		
		private Integer getIntForResource(String resource) {
			if (this.resources.containsKey(resource))
				return this.resources.get(resource);
			
			this.resources.put(resource, this.lastResourceId);
			this.resourceIds.put(this.lastResourceId, resource);
			
			return this.lastResourceId++;
		}
		
		private String getResourceForInt(Integer key) {
			return this.resourceIds.get(key);
		}
		private void writePageRankResultsToFile(Grph graph,PageRank ranker,String graphType)
		{
			try
			{
				File file = new File(input.getParent() + "/"+graphType+"_scores_en_grph.ttl");
	
				// if file doesnt exists, then create it
				if (!file.exists()) {
					file.createNewFile();
				}
	
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(
						file.getAbsoluteFile()), "UTF-8");
				BufferedWriter bw = new BufferedWriter(fw);
				IntSet set= graph.getVertices();
	        	Iterator<IntCursor> itr =set.iterator();
	        	while(itr.hasNext())
				{
	        		IntCursor cursor =itr.next();
					bw.write("<http://dbpedia.org/resource/" + getResourceForInt(cursor.value)+ ">"
							+ " <http://dbpedia.org/ontology/wikiPageRank> \""
							+ ranker.getRank(cursor.index)
							+ "\"^^<http://www.w3.org/2001/XMLSchema#float> .");
					bw.write("\n");
				}
	        	
	        	bw.close();
				fw.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private void writeEccentricityResultsToFile(Grph graph,String graphType)
		{
			try
			{
				File file = new File(input.getParent() + "/"+graphType+"_scores_en_grph.ttl");
				// if file doesnt exists, then create it
				if (!file.exists()) {
					file.createNewFile();
				}
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(
						file.getAbsoluteFile()), "UTF-8");
				BufferedWriter bw = new BufferedWriter(fw);
				IntSet set= graph.getVertices();
            	Iterator<IntCursor> itr =set.iterator();
            	while(itr.hasNext())
    			{
            		IntCursor cursor =itr.next();
    				bw.write("<http://dbpedia.org/resource/" + getResourceForInt(cursor.value)+ ">"
    						+ " <http://dbpedia.org/ontology/wikiEccentricity> \""
    						+ graph.getEccentricity(cursor.index)
    						+ "\"^^<http://www.w3.org/2001/XMLSchema#float> .");
    				bw.write("\n");
    			}
	        	
	        	bw.close();
				fw.close();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}

