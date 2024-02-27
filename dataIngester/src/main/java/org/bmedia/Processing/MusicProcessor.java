package org.bmedia.Processing;

public class MusicProcessor extends MediaProcessor<String>{

    /**
     * Main constructor
     *
     * @param group {@link ProcessingGroup} to process for
     */
    protected MusicProcessor(ProcessingGroup group) {
        super(group);
    }

    @Override
    public void atomicProcessingImplementation() {

    }

}
