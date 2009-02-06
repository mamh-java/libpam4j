package org.jvnet.libpam;

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import static org.jvnet.libpam.impl.CLibrary.libc;
import org.jvnet.libpam.impl.CLibrary.passwd;
import org.jvnet.libpam.impl.CLibrary.group;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an Unix user. Immutable.
 *
 * @author Kohsuke Kawaguchi
 */
public final class UnixUser {
    private final String userName;
    private final int uid,gid;
    private final Set<String> groups;

    /*package*/ UnixUser(String userName, passwd pwd) throws PAMException {
        this.userName = userName;
        this.uid = pwd.pw_uid;
        this.gid = pwd.pw_gid;

        int sz = 4; /*sizeof(gid_t)*/

        int ngroups = 64;
        Memory m = new Memory(ngroups*sz);
        IntByReference pngroups = new IntByReference(ngroups);
        if(libc.getgrouplist(userName,pwd.pw_gid,m,pngroups)<0) {
            // allocate a bigger memory
            m = new Memory(pngroups.getValue()*sz);
            if(libc.getgrouplist(userName,pwd.pw_gid,m,pngroups)<0)
                // shouldn't happen, but just in case.
                throw new PAMException("getgrouplist failed");
        }

        ngroups = pngroups.getValue();
        groups = new HashSet<String>();
        for( int i=0; i<ngroups; i++ ) {
            int gid = m.getInt(i * sz);
            group grp = libc.getgrgid(gid);
            groups.add(grp.gr_name);
        }
    }

    /**
     * Gets the unix account name. Never null.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Gets the UID of this user.
     */
    public int getUID() {
        return uid;
    }

    /**
     * Gets the GID of this user.
     */
    public int getGID() {
        return gid;
    }

    /**
     * Gets the groups that this user belongs to.
     *
     * @return
     *      never null.
     */
    public Set<String> getGroups() {
        return Collections.unmodifiableSet(groups);
    }
}
