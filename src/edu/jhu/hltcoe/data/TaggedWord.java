package edu.jhu.hltcoe.data;


public class TaggedWord implements Label {

    private String word;
    private String tag;
    private int position;
    
    public TaggedWord(String word, String tag, int position) {
        this.word = word;
        this.tag = tag;
        this.position = position;
    }

    public String getWord() {
        return word;
    }

    public String getTag() {
        return tag;
    }

    @Deprecated
    public int getPosition() {
        return position;
    }
    
    public String getLabel() {
        // Must also update hashCode and equals if changing this
        return tag;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
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
        TaggedWord other = (TaggedWord) obj;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        return true;
    }
    

}