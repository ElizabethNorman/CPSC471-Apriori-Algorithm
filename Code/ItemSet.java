/**
 * The ItemSet class is responsible for containing the ID and count of a particular candidate for use in my Apriori
 * algorithm.
 *
 * This small class is the second class needed to run my Apriori program. The other class is Apriori.java, please ensure
 * you have both in your folder to properly run this program
 *
 * @author Elizabeth Norman
 * February 11th, 2023
 */


public class ItemSet {

    //each itemset object has two class variables
    private int count;
    private final String id;

    /**
     * One of two constructors for the ItemSet class, this one does not need a count to create an itemset object
     * @param id
     */
    public ItemSet(String id)
    {
        this.id = id;
        this.count = 1;
    }

    /**
     * The other constructor for the ItemSet class, this constructor does require a count to create object
     * @param id
     * @param count
     */
    public ItemSet(String id, int count)
    {
        this.id = id;
        this.count = count;
    }

    /**
     * Simply increases the count of the particular itemset
     */
    public void increaseCount()
    {
        count ++;
    }

    /**
     * @return count of itemset
     */
    public int getCount()
    {
        return count;
    }

    /**
     * @return the ID of itemset.
     */
    public String getId()
    {
        return id;
    }
}
