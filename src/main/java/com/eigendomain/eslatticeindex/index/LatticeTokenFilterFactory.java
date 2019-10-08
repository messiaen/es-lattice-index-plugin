/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eigendomain.eslatticeindex.index;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;


public class LatticeTokenFilterFactory extends AbstractTokenFilterFactory {
    private SortedMap<Float, Integer> buckets;

    private final String latticeFormat;
    private final float audioSecondsPositionIncrementInterval;
    // TODO
    // private final int imagePixelPositionIncrementInterval;
    private final String fieldDelimiter;

    public LatticeTokenFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, name, settings);
        this.buckets = parseBucketList(settings.getAsList("score_buckets"));
        this.fieldDelimiter = settings.get("field_delimiter", "|");
        this.latticeFormat = settings.get("lattice_format", "lattice");
        this.audioSecondsPositionIncrementInterval = settings.getAsFloat("audio_position_increment_seconds", 0.01f);
    }

    @Override
    public LatticeTokenFilter<?> create(TokenStream input) {
        if (this.fieldDelimiter.length() != 1) {
            throw new IllegalArgumentException("'field_delimiter' must be a single character");
        }
        char delim = this.fieldDelimiter.toCharArray()[0];

        return new LatticeTokenFilter<>(input, this.buckets, delim, getLatticeTokenPartsFactory());
    }

    private LatticeTokenPartsFactory<?> getLatticeTokenPartsFactory() {
        String format = this.latticeFormat.toLowerCase(Locale.ROOT);
        switch (format) {
            case "lattice":
                //System.out.println("Creating lattice format");
                return new BaseLatticeTokenParts.Factory();
            case "audio":
                //System.out.println("Creating audio format");
                return new AudioLatticeTokenParts.Factory(this.audioSecondsPositionIncrementInterval);
        }
        throw new IllegalArgumentException("Invalid lattice format '" + latticeFormat + "'");
    }

    private SortedMap<Float, Integer> parseBucketList(List<String> bucketsStrings) {
        SortedMap<Float, Integer> buckets = new TreeMap<>(new Comparator<Float>() {
            @Override
            public int compare(Float o1, Float o2) {
                float d = o1 - o2;
                if (d < 0.0) {
                    return 1;
                } else if (d > 0.0) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        float threshold = 1.0f;
        for (int i = 0; i < bucketsStrings.size(); ++i) {
            if (i % 2 == 0) {
                threshold = Float.parseFloat(bucketsStrings.get(i));
                buckets.put(threshold, 1);
            } else {
               buckets.put(threshold, Integer.parseInt(bucketsStrings.get(i)));
            }
        }

        return buckets;
    }
}
