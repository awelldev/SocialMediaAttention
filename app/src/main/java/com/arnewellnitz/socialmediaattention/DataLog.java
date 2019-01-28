package com.arnewellnitz.socialmediaattention;


import java.util.List;

public class DataLog {

    Section[] sectionList;
    int mSections;
    int mAbsoluteHeight;
    int mSegmentHeight; // height of photo + spacer
    int mElements;

    public DataLog(int elements, int sections, int absHeight, int segmentHeight) {
        mElements = elements;
        mSections = sections;
        mAbsoluteHeight = absHeight;
        mSegmentHeight = segmentHeight;
        sectionList = new Section[mElements*mSections];
        for(int i=0; i<sectionList.length; i++) {
            sectionList[i] = new Section();
        }
    }


    void setSegmentTime(int distance) {
        int index = distance/mSegmentHeight;
        sectionList[index].mTime += 10;
    }


    public class Section {
        public int top = 0;
        public long mTime = 0;
        int mCounter = 0;

        public int getTop() {
            return top;
        }

        public void setTop(int top) {
            this.top = top;
        }

        public long getmTime() {
            return mTime;
        }

        public void setmTime(long mTime) {
            this.mTime = mTime;
        }

        public int getmCounter() {
            return mCounter;
        }

        public void setmCounter(int mCounter) {
            this.mCounter = mCounter;
        }
    }
}
