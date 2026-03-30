package com.talhanation.recruits.entities;

public interface IHasTargetPriority {
    void setTargetPriority(TargetPriority x);
    int getTargetPriority();

    enum TargetPriority {
        CLOSEST(0),
        INFANTRY(1),
        CAVALRY(2),
        SIEGE_WEAPONS(3),
        SHIPS(4);


        private final int index;
        TargetPriority(int index){
            this.index = index;
        }

        public int getIndex(){
            return this.index;
        }

        public static TargetPriority fromIndex(int index) {
            for (TargetPriority prio : TargetPriority.values()) {
                if (prio.getIndex() == index) {
                    return prio;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }
    }
}
