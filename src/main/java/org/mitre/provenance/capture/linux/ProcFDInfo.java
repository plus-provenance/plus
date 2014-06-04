package org.mitre.provenance.capture.linux;

/**
 * Small utility class representing the various flags that can be attached to a file descriptor.
 * See http://man7.org/linux/man-pages/man2/open.2.html for more information on flags.
 * <p>This comes from /proc/XX/fdinfo/YY where XX is a process PID, and YY is a file descriptor.
 * 
 * <p>Some flag definitions, taken from fnctl.h on linux:
 * <pre>
 * # define O_CREAT           0100
 * # define O_APPEND         02000
 * #define O_ACCMODE          0003
 * #define O_RDONLY             00
 * #define O_WRONLY             01
 * #define O_RDWR               02
 * # define O_CREAT           0100
 * # define O_TRUNC          01000
 * # define __O_DIRECTORY  0200000
 * # define O_DIRECTORY    __O_DIRECTORY
 * # define O_NONBLOCK       04000
 * # define __O_CLOEXEC   02000000
 * </pre>
 * 
 * @author moxious
 *
 */
public class ProcFDInfo {
	protected char [] flags = null;
	protected long pos = -1;
	
	protected static final Character C_0 = new Character('0');
	protected static final Character C_1 = new Character('1');
	protected static final Character C_2 = new Character('2'); 
	protected static final Character C_3 = new Character('3'); 
	protected static final Character C_4 = new Character('4'); 
	
	public ProcFDInfo(String pos, String flags) {
		// Flags strings look like for example 02004002.
		// We reverse the string and split it into characters, so it's easy to look up the nth
		// position, regardless of how long the string is.   
		this.flags = new StringBuilder(flags).reverse().toString().toCharArray();				
		this.pos = Long.parseLong(pos);
	} 
	
	/**
	 * Get the original set of flags (in normal order) provided to this object.
	 */
	public String getFlags() { return new StringBuilder(new String(flags)).reverse().toString(); }
	public long getPos() { return pos; }
	
	/**
	 * Return the nth character flag in flags, or null if it doesn't exist/is out of range.
	 */
	protected Character nth(int n) { if(flags.length <= n) return null; return flags[n]; }
		
	public boolean O_WRONLY() { return C_1.equals(nth(0)); } 
	public boolean O_RDWR() { return C_2.equals(nth(0)); } 
	public boolean O_RDONLY() { return C_0.equals(nth(0)); } 
	public boolean O_APPEND() { return C_2.equals(nth(3)); }  
	public boolean O_TRUNC() { return C_1.equals(nth(4)); } 
	public boolean O_DIRECTORY() { return C_2.equals(nth(5)); } 
	public boolean O_CREAT() { return C_1.equals(nth(2)); }
	public boolean O_NONBLOCK() { return C_4.equals(nth(3)); }	
} // End ProcFDInfo
