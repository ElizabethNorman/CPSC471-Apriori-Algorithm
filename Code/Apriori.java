import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * This is the main class of my Apriori algorithm produced for CPSC 471, Data Mining. This Apriori class will take in user
 * data as the program starts, validates it, and the proceeds with the Apriori algorithm. Each method is heavily commented
 * as to explain how my algorithm works and what each part is used for.
 *
 * There are two classes involved in this algorithm, this is the first. Ensure that ItemSet.java is also included the folder
 * for proper running of the program.
 *
 * @author Elizabeth Norman
 * February 10th, 2022
 */

public class Apriori
{
    private int fpCount;
    private int numLines;
    private final String filename;
    private final double threshold;

    /** Main calls the validateInput() method to ensure the marker entered a correct format for successful running of
     * the program and then runs the Apriori algorithm.
     *
     * @param args the arguments provided during program start
     * @throws FileNotFoundException calling of Apriori necessitates this exception handling
     */
    public static void main (String [] args) throws FileNotFoundException
    {
        boolean valid = validateInput(args);

        if (valid)
        {
            long start = System.currentTimeMillis();
            Apriori ap = new Apriori(args[0], Double.parseDouble(args[1]));
            long end = System.currentTimeMillis();
            String output = ("Total Runtime: " + ((double)(end - start)/1000) + " seconds");
            log(output);
        }
        else
        {
            log("Incorrect input was given. Run again and follow the format of: \"java Main filename.txt 0.xx\"");
            log("where xx represents a percentage. IE, 3% is 0.03");
        }
    }

    /** The Apriori constructor handles the filename and threshold to be sent to the Apriori algorithm
     * Also responsible for outputting the number of frequent patterns found
     *
     * @param filename name of file
     * @param threshold minimum support threshold
     * @throws FileNotFoundException calls methods that write/read to/from files
     */
    public Apriori(String filename, double threshold) throws FileNotFoundException
    {

        fpCount = 0;
        this.filename = filename;
        this.threshold = threshold;
        runApriori();

        log("|FPs| = " + fpCount);
    }

    /**
     * This is the main portion of my Apriori algorithm. We start with creating table C1, then proceed through the rest
     * of the candidate tables until no more can possibly be created.
     *
     * @throws FileNotFoundException calls methods that read/write from/to files
     */
    private void runApriori() throws FileNotFoundException
    {

        //create c1 table
        LinkedHashMap<String, ItemSet> candidates = createCandidateTable1();

        //prune table
        pruneMin(candidates);

        //create frequent patterns table for later use for writing to a file
        //we use a linked hashmap to retain order for printing purposes
        Map<String, ItemSet> FPs = new LinkedHashMap<>(candidates);

        //a boolean to keep program running until we are done
        boolean run = true;

        //what c tables we are starting with. start at 2 as 1 has already been created
        int cs = 2;

        while(run)
        {

            ArrayList<String> lX = createLXTable(candidates);

            LinkedHashMap<String, ItemSet> tempCandidates = createCXs(lX, cs);

            pruneMin(tempCandidates);

            FPs.putAll(tempCandidates);

            candidates.clear();
            candidates.putAll(tempCandidates);

            cs++;

            if (candidates.size() == 0)
            {
                run = false;
            }

        }
        fpCount = FPs.size();
        writeToFile(FPs);
    }


    /**
     * There is no need to compare each itemset in the cX table to the database, this is too expensive, timewise. Because
     * I was mindful to sort each candidate as they were created in join, what createBuckets does is creates a "bucket"
     * where the candidates are placed into the bucket based on the first item of the itemset.
     *
     * Example: If itemsets are
     *
     * 1, 2, 3
     * 1, 4, 5
     * 1, 3, 4
     *
     * 2, 4, 5
     * 2, 3, 4
     *
     * And so on, we will only now send 2 items to be scanned to the database instead of 5.
     *
     * The usefulness of this is explained further in readData
     *
     * @param cTable the candidate table
     * @throws FileNotFoundException as we call readData, this exception must be called
     */
    private void createBuckets(LinkedHashMap<String, ItemSet> cTable) throws FileNotFoundException
    {

        HashMap<Integer, ArrayList<ItemSet>> buckets = new HashMap<>();

        for (Map.Entry<String, ItemSet> m : cTable.entrySet())
        {

            String[] temp = m.getKey().split(" ");

            //all buckets are defined by the first value of the itemset, this is why we can hardcode [0]
            if (!buckets.containsKey(Integer.parseInt(temp[0])))
            {
                buckets.put(Integer.parseInt(temp[0]), new ArrayList<>());
            }
            //we add the itemset to the bucket's ArrayList of itemsets
            buckets.get(Integer.parseInt(temp[0])).add(m.getValue());
        }

        //we call readData to get counts
        readData(buckets);

        //clear the table that was sent to this method
        cTable.clear();

        //refill the cTable with the counted values from our bucket call
        for (Map.Entry<Integer, ArrayList<ItemSet>> m : buckets.entrySet())
        {
            for (int i = 0; i < m.getValue().size(); i ++)
            {
                ItemSet temp = m.getValue().get(i);
                cTable.put(temp.getId(), new ItemSet(temp.getId(), temp.getCount()));
            }
        }
    }

    /**
     * readData is responsible for scanning the database and updating candidates with counts if the transaction contains
     * the items within an itemset.
     *
     * In createBuckets, we divided up potential candidates into buckets defined by the minimum item of the itemsets.
     * When cycling through the file, if the minimum item isn't even contained in the transaction, we skip the entire
     * bucket. This saves *buckets* of time. I'll see myself out.
     *
     * To the observant eye, this has a O(N^3) with the 3 nested loops. However, the work done above in createBuckets()
     * cuts down on time spent considerably.
     *
     *
     * @param buckets the divided up containers containing itemsets
     * @throws FileNotFoundException the method will be reading from file
     */
    private void readData(HashMap<Integer, ArrayList<ItemSet>> buckets) throws FileNotFoundException
    {
        File f = new File(filename);
        Scanner sc = new Scanner(f);
        sc.nextLine();

        String data;
        String[] dataSplit;
        String[] transaction;

        //begin database scan
        while (sc.hasNextLine())
        {
            data = sc.nextLine();
            dataSplit = data.split("\t");
            transaction = dataSplit[2].split(" ");

            //create arraylist out of the transaction
            ArrayList<String> transactionAL = new ArrayList<>(Arrays.asList(transaction));

            //cycle through map of buckets
            for (Map.Entry<Integer, ArrayList<ItemSet>> s : buckets.entrySet())
            {

                //if key of transaction is not in that line, skip entire bucket
                if (!transactionAL.contains(s.getKey().toString()))
                {
                    continue;
                }

                //otherwise, go through the bucket and find itemsets that are contained in transaction
                for (int i = 0; i < s.getValue().size(); i ++)
                {
                    ArrayList<String> temp2 = new ArrayList<>(Arrays.asList(s.getValue().get(i).getId().split(" ")));

                    //if a match is found then you increase the itemsets count
                    if (transactionAL.containsAll(temp2))
                    {
                        s.getValue().get(i).increaseCount();
                    }
                }
            }
        }
    }

    /** Candidate Table 1 has to be treated differently than other C tables, as we need to scan the database to first create it
     * as there are no previous L tables to build it from. This simply scans the database and either
     * 1) adds a new itemset to C1 with an initial count of 1
     * 2) increases that itemset's count within the C1 table
     *
     * @return HashMap representing C1 table
     * @throws FileNotFoundException we are scanning the database
     */
    private LinkedHashMap<String, ItemSet> createCandidateTable1() throws FileNotFoundException
    {

        File f = new File(filename);
        Scanner sc = new Scanner(f);
        numLines = Integer.parseInt(sc.nextLine());

        LinkedHashMap<String, ItemSet> itemSets = new LinkedHashMap<>();


        while (sc.hasNextLine())
        {
            String data = sc.nextLine();
            String[] s = data.split("\t");
            String[] items = s[2].split(" ");

            for (String temp : items)
            {
                if (itemSets.containsKey(temp)) {
                    itemSets.get(temp).increaseCount();
                } else {
                    itemSets.put(temp, new ItemSet(temp));
                }
            }
        }
        return itemSets;
    }


    /**
     * createCXs creates candidate tables of size n from the previous LX table and returns a new HashMap
     *
     *
     * @param a the arrayList to be turned into a candidate table
     * @param n the size of candidate table we are making
     * @return the hashmap of newly counted itemsets
     * @throws FileNotFoundException as we lead into reading the database, this exception is necessary
     */
    private LinkedHashMap<String, ItemSet> createCXs(ArrayList<String> a, int n) throws FileNotFoundException
    {

        //self join the arraylist to find new candidates
        ArrayList<String> ta = selfJoin(a, n);

        //place the new candidates into a hashmap
        LinkedHashMap<String, ItemSet> tc = new LinkedHashMap<>();

        for (String s: ta)
        {
            tc.put(s, new ItemSet(s));
        }

        //call create buckets to divide up candidates into buckets and then count their occurrences
        createBuckets(tc);

        return tc;
    }

    /**
     * This helper method just converts Maps to ArrayLists
     *
     * When creating new C tables and joining candidates together I rely on ArrayLists, which admittedly should be refined
     *  and would be in the land where I have infinite time.
     *
     * @param itemSets itemset to be converted into array list
     * @return arraylist is returned with all the itemsets
     */
    private ArrayList<String> createLXTable(Map<String, ItemSet> itemSets)
    {
        ArrayList<String> ta2 = new ArrayList<>();

        for (Map.Entry<String, ItemSet> m : itemSets.entrySet())
        {
            ta2.add(m.getKey());
        }
        return ta2;
    }

    /**
     * The selfJoin method is responsible for creating the new candidate table based on the previously pruned LX table.
     * This is not as efficient as one may desire, but it works.
     *
     * @param s L Table to make new candidate table with
     * @param n size of joins we are doing, ie, item sets of size 2, 3,... n
     * @return ArrayList of new potential candidates to be counted
     */
    private ArrayList<String> selfJoin(ArrayList<String> s, int n)
    {
        ArrayList<String> joined = new ArrayList<>();

        //we compare each itemset to the next itemset
        for (int i = 0; i < s.size(); i++)
        {
            for (int j = i+1; j < s.size(); j++)
            {

                //Since we are concatenating strings, two StringBuilders are needed
                StringBuilder s1 = new StringBuilder();
                StringBuilder s2 = new StringBuilder();

                //Utilize StringTokenizers to parse the itemsets
                StringTokenizer st1 = new StringTokenizer(s.get(i));
                StringTokenizer st2 = new StringTokenizer(s.get(j));

                //Make two strings out of the 0 to n-2 tokens of the strings
                for (int k = 0; k < n - 2; k++)
                {
                    s1.append(" ").append(st1.nextToken());
                    s2.append(" ").append(st2.nextToken());
                }

                //if the strings have same tokens, add together
                if(s2.toString().compareToIgnoreCase(s1.toString())==0)
                {
                    String itemset = s1 + " " + st1.nextToken() + " " +st2.nextToken().trim();

                    //trim off any excess, or it breaks entirely :)
                    itemset = itemset.trim();

                    //The crux of my algorithm relies on a sorted itemset, so we sort it before placing into table.
                    itemset = sortCandidate(itemset);

                    //finally, we add the itemset to the arraylist to be returned.
                    joined.add(itemset);
                }
            }
        }
        return joined;
    }

    /**
     * This is an admittedly slow way to sort the individual itemset, but it's what my algorithm relies on. With infinite
     * time and resources I would make this better.
     *
     * @param s the itemset to be sorted
     * @return a sorted string
     */
    private String sortCandidate(String s)
    {
        ArrayList<Integer> se = new ArrayList<>();
        String[] see = s.split(" ");
        StringBuilder d = new StringBuilder();
        for (String x : see)
        {
            se.add(Integer.parseInt(x));
        }
        Collections.sort(se);
        for (Integer x : se)
        {
            d.append(x).append(" ");
        }
        d = new StringBuilder(d.toString().trim());

        return d.toString();
    }

    /**
     * This tiny method just prunes the candidates that don't meet the minimum support threshold. While only
     * being one line, since it would otherwise be written multiple times in runApriori(), it was proper to create
     * a method instead.
     *
     * @param cTable
     */
    private void pruneMin(Map<String, ItemSet> cTable)
    {
        cTable.entrySet().removeIf(entry -> entry.getValue().getCount() < (numLines)*threshold);
    }


    /**
     * writeToFile is responsible for writing to the MiningResult file as specified by the assignment guidelines.
     *
     * So I forgot the commas until, like, submitting time so this is pretty messy. I apologize. But this prints to file, and it
     * works just fine, thank you.
     *
     * @param FPs the frequent patterns to be written to file
     * @throws FileNotFoundException needed as we are writing to file
     */
    private void writeToFile(Map<String, ItemSet> FPs) throws FileNotFoundException
    {
        PrintWriter fileOutput = new PrintWriter("MiningResult.txt");
        fileOutput.println("|FPs| = " + FPs.size());

        for (Map.Entry<String, ItemSet> fp : FPs.entrySet())
        {
            String[] fpss = fp.getKey().split(" ");
            fileOutput.print(fpss[0]);
            if (fpss.length > 1)
            {
                fileOutput.print(", ");
            }
            for (int i = 1; i < fpss.length-1; i ++)
            {
                fileOutput.print(fpss[i] + ", ");
            }
            if (fpss.length > 1)
            {
                fileOutput.print(fpss[fpss.length-1]);
            }
            fileOutput.print( " : " + fp.getValue().getCount());
            fileOutput.println();
        }
        fileOutput.close();
    }

    /**
     * The validateInput method is responsible for ensuring that the input put into the command line
     * is valid. This is not as robust as could possibly be, as I am assuming the marker will follow my very nicely
     * written and detailed instructions as provided in ReadMe.txt
     *
     * @param args command line arguments
     */
    private static boolean validateInput(String[] args)
    {
        if (args.length != 2)
        {
            return false;
        }

        File f = new File(args[0]);

        if (!f.isFile())
        {
            log("File not found.");
            return false;
        }

        double secondArg;
        try
        {
            secondArg = Double.parseDouble(args[1]);
        }
        catch (NumberFormatException e)
        {
            log("Second argument is not a number.");
            return false;
        }

        if (secondArg > 0.99 || secondArg < 0.01)
        {
            log("Second argument is not a valid percent. Insert a number between 0.01 and 0.99");
            return false;
        }
        return true;
    }

    /**
     * This method is just a sanity saving method when working in Java, by letting me type 3 characters to  print
     * to command line instead of 18 :)
     *
     * @param s String to be printed
     */
    private static void log(String s)
    {
        System.out.println(s);
    }
}