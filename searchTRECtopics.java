import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import java.util.Comparator;

public class searchTRECtopics
{	
	public static <K extends Comparable,V extends Comparable> Map<K,V> sortByValues(Map<K,V> map)
	{
        List<Map.Entry<K,V>> entries = new LinkedList<Map.Entry<K,V>>(map.entrySet());
        
        Collections.sort(entries, new Comparator<Map.Entry<K,V>>() 
        {

			@Override
			public int compare(Entry<K, V> o1, Entry<K, V> o2) 
			{
				return o2.getValue().compareTo(o1.getValue());
			}

            
        });
     
        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<K,V> sortedMap = new LinkedHashMap<K,V>();
     
        for(Map.Entry<K,V> entry: entries)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
     
        return sortedMap;
    }

	static String extract(StringBuilder buf, String startTag, String endTag)
	{
		String stringBetweenTags = new String();
		int k1 = buf.indexOf(startTag);
		while(k1 > 0)    
		{
		   k1 += startTag.length();
		   int k2 = buf.indexOf(endTag,k1);
		      
		   if (k2>=0)
		   {
			   stringBetweenTags +=(" " + buf.substring(k1,k2).trim());  
		   }
		   
		   k1 = buf.indexOf(startTag, k2);
		}
		return stringBetweenTags;	  
	}
	
	static String readFile(String file) throws IOException 
	{
		FileReader fileReader = new FileReader (file);
		BufferedReader reader = new BufferedReader(fileReader);
	    String         line = null;
	    StringBuilder  stringBuilder = new StringBuilder();
	    String         ls = System.getProperty("line.separator");

	    while((line = reader.readLine()) != null ) 
	    {
	        stringBuilder.append( line );
	        stringBuilder.append( ls );
	    }

	    return stringBuilder.toString();
	}
	
	static void collectQuery(String filePath, List<String> shortQuery,List<String> longQuery) throws IOException
	{
		String readBuffer = readFile(filePath);
		StringBuilder builder = new StringBuilder(readBuffer);
		
		String startDocTag = "<top>";
		String endDocTag = "</top>";
		int docStart = builder.indexOf(startDocTag);
		while(docStart != -1)    
		{
		   docStart += startDocTag.length();
		   int docEnd = builder.indexOf(endDocTag,docStart);
		   
		   if(docEnd > 0)
		   {
			   StringBuilder document = new StringBuilder(builder.substring(docStart,docEnd).trim());
			   String doctitle = extract(document,"<title>", "<desc>");
			   String dateDesc = extract(document,"<desc>", "<smry>");
			   shortQuery.add(doctitle);
			   longQuery.add(dateDesc);
		   }
		   docStart = builder.indexOf(startDocTag, docEnd);
		}
	}
	
	static void parseQuery(String indexpath, List<String> queries, File trecFilePath) throws IOException, ParseException
	{
		//queries.size()
		for(int queryID = 0 ; queryID < queries.size(); ++queryID)
		{
			Map<String,Float> docScorePair = new HashMap<String,Float>();
			String queryString = queries.get(queryID).replace("Topic:", "").replace("/", "").replace("\\", "").replace("?", "");
			IndexReader indexReader = DirectoryReader.open(FSDirectory.open(new File(indexpath)));
			IndexSearcher searcher = new IndexSearcher(indexReader);
			long totalNumberOfDoc = indexReader.maxDoc();
			System.out.println(queryID);
			
			Analyzer analyzer = new StandardAnalyzer();
			QueryParser parser = new QueryParser("TEXT",analyzer); // checking in only TEXT field of the documents
			Query query = parser.parse(queryString);
			
			Set<Term> queryTerms = new LinkedHashSet<Term>(); 
			query.extractTerms(queryTerms);
			
			DefaultSimilarity defaultSimilarity = new DefaultSimilarity();
			List<AtomicReaderContext> leafContexts = indexReader.getContext().reader().leaves();
			
			HashMap<Integer,HashMap<Integer,Integer>> docQueryMap = new HashMap<Integer,HashMap<Integer,Integer>>();
			
			for(int leaf = 0; leaf < leafContexts.size() ; ++leaf )
			{
				AtomicReaderContext leafContext = leafContexts.get(leaf);
				int startDocNumber = leafContext.docBase;
				int queryNumber = 0;
				for(Term t: queryTerms)
				{
					++queryNumber;
					int doc = 0;
					DocsEnum docEnum = MultiFields.getTermDocsEnum(leafContext.reader(), MultiFields.getLiveDocs(leafContext.reader()), "TEXT", new BytesRef(t.text()));
					if(docEnum == null)
					{
						break;
					}
					while((doc = docEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS)
					{
						if(!docQueryMap.containsKey(docEnum.docID()+startDocNumber))
						{
							HashMap<Integer,Integer> queryCount = new HashMap<Integer,Integer>();
							docQueryMap.put(docEnum.docID()+startDocNumber, queryCount);
						}
						HashMap<Integer,Integer> queryCount = docQueryMap.get(docEnum.docID()+startDocNumber);
						queryCount.put(queryNumber, docEnum.freq());	
					}
				}
			}
			
			
			for(int leaf = 0; leaf < leafContexts.size() ; ++leaf )
			{
				AtomicReaderContext leafContext = leafContexts.get(leaf);
				
				int startDocNumber = leafContext.docBase;
				int numberOfDoc = leafContext.reader().maxDoc();
				for(int startDoc = startDocNumber; startDoc < (startDocNumber + numberOfDoc) ; ++startDoc)
				{ 
					float rank = 0;
					float normDocLength = defaultSimilarity.decodeNormValue(leafContext.reader().getNormValues("TEXT").get(startDoc-startDocNumber));
					if(docQueryMap.containsKey(startDoc))
					{
						int queryNumber = 0;	
						for(Term t: queryTerms)
						{
							++queryNumber;
							long numberOfdocsHaveQueryt = indexReader.docFreq(new Term("TEXT",t.text()));
						
							HashMap<Integer,Integer> queryCount = docQueryMap.get(startDoc);
							if(queryCount.containsKey(queryNumber))
							{
								rank+= (queryCount.get(queryNumber)/normDocLength)*Math.log10((1+(totalNumberOfDoc/numberOfdocsHaveQueryt)));
							}
						}
					}
					String docNum =  searcher.doc(startDoc).get("DOCNO");
					docScorePair.put(docNum, rank);
				}
			}
			Map<String,Float> sorted = sortByValues(docScorePair);
			//queryMap.add(sorted);
			outputTrec_Eval(queryID,sorted,trecFilePath);
		}
	}
	
	public static void outputTrec_Eval(int queryID,Map<String,Float> shortQueryMap, File trecFilePath) throws IOException
	{
		FileWriter fileWriter = new FileWriter(trecFilePath,true);
		
		int j = 0;
		for (Map.Entry<String, Float> entry : shortQueryMap.entrySet())
		{
			if(entry.getValue() == 0) break;
			fileWriter.write((51+queryID)+"     Q0      "+entry.getKey()+"    "+(++j)+"   "+entry.getValue()+"   run-l \n");
			if(j == 1000) break;
		}
		fileWriter.close();
	}
	
	public static void main(String[] args) throws IOException, ParseException
	{
		String filePath = args[0];
		String indexPath = args[1];
		String trecFilePath = args[2];
		List<String> shortQuery = new ArrayList<String>();
		List<String> longQuery = new ArrayList<String>();
		
		//List<Map<String,Float>> shortQueryMap = new ArrayList<Map<String,Float>>();
		//List<Map<String,Float>> longQueryMap = new ArrayList<Map<String,Float>>();
		File trecFilePathShort = new File(trecFilePath+"//trec_eval_short.txt");
		FileWriter fileWriter = new FileWriter(trecFilePathShort);
		fileWriter.write("");
		File trecFilePathLong = new File(trecFilePath+"//trec_eval_long.txt");
		fileWriter = new FileWriter(trecFilePathLong);
		fileWriter.write("");
		
		collectQuery(filePath,shortQuery,longQuery);
		parseQuery(indexPath,shortQuery,trecFilePathShort);
		parseQuery(indexPath,longQuery,trecFilePathLong);
				
		//outputTrec_Eval(shortQueryMap,longQueryMap,trecFilePath);
		System.out.println("Done");

	}
}
