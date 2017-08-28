package com.projectlounge.bean;

import com.projectlounge.utils.Hashes;
import com.projectlounge.utils.Hashes4;
import com.projectlounge.utils.HashesAny;
import org.springframework.stereotype.Component;

/**
 * Created by main on 08.08.17.
 */
@Component
public class Create {

    public Hashes hashes(final byte[] hashes, final int prefixSize) {
        if (prefixSize==4) return new Hashes4(hashes);
        return new HashesAny(hashes, prefixSize);
    }

}
