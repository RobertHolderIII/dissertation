package hu.pj.obj;

import java.io.Serializable;

/**
 * adapted from from http://rosettacode.org/wiki/Knapsack_problem/0-1#Java
 *
 *
 */
public class Item implements Serializable{

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	protected String name    = "";
    protected int weight     = 0;
    protected int value      = 0;
    protected int bounding   = 1; // the maximum quanity of item in knapsack
    protected int inKnapsack = 0; // the pieces of item in solution
    protected boolean variable = false; //true if this item weight and value vary within problem instances
    public Item() {}



    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + value;
		result = prime * result + weight;
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item other = (Item) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value != other.value)
			return false;
		if (weight != other.weight)
			return false;
		return true;
	}



	public Item(Item item) {
        setName(item.name);
        setWeight(item.weight);
        setValue(item.value);
        setBounding(item.bounding);
        setVariable(item.variable);
    }


//    public Item(int _weight, int _value) {
//        setWeight(_weight);
//        setValue(_value);
//    }
//
//    public Item(int _weight, int _value, boolean _variable) {
//        this(_weight,_value);
//        setVariable(_variable);
//    }
//
//
//    public Item(int _weight, int _value, int _bounding) {
//        setWeight(_weight);
//        setValue(_value);
//        setBounding(_bounding);
//    }

    public Item(String _name, int _weight, int _value, boolean _variable) {
        this(_name, _weight, _value);
        setVariable(_variable);
    }


    public Item(String _name, int _weight, int _value) {
        setName(_name);
        setWeight(_weight);
        setValue(_value);
    }

    public Item(String _name, int _weight, int _value, int _bounding) {
        setName(_name);
        setWeight(_weight);
        setValue(_value);
        setBounding(_bounding);
    }

    public Item(String _name, int _weight, int _value, int _bounding, boolean _variable) {
        this(_name, _weight, _value, _bounding);
        setVariable(_variable);
    }

    public void setName(String _name) {name = _name;}
    public void setWeight(int _weight) {weight = Math.max(_weight, 0);}
    public void setValue(int _value) {value = Math.max(_value, 0);}
    public void setVariable(boolean _variable) {variable = _variable;}

    public void setInKnapsack(int _inKnapsack) {
        inKnapsack = Math.min(getBounding(), Math.max(_inKnapsack, 0));
    }

    public void setBounding(int _bounding) {
        bounding = Math.max(_bounding, 0);
        if (bounding == 0)
            inKnapsack = 0;
    }

    public void checkMembers() {
        setWeight(weight);
        setValue(value);
        setBounding(bounding);
        setInKnapsack(inKnapsack);
    }

    public String getName() {return name;}
    public int getWeight() {return weight;}
    public int getValue() {return value;}
    public int getInKnapsack() {return inKnapsack;}
    public int getBounding() {return bounding;}
    public boolean isVariable(){return variable;}

    @Override
	public String toString(){
    	return "Item[" + getName() + ": value=" + getValue() + ", weight=" + getWeight() + ", inKnapsack=" + getInKnapsack() + "]";
    }

} // class