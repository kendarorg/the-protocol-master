package org.kendar.server;

import org.kendar.buffers.BBuffer;
import org.kendar.protocol.BytesEvent;
import org.kendar.protocol.ProtoStep;
import org.kendar.protocol.ReturnMessage;
import org.kendar.protocol.fsm.ProtoState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SocketChannel {
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;

    public SocketChannel(Socket socket){

        this.socket = socket;
        try {
            this.out = new DataOutputStream(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(BBuffer buffer){
        buffer.setPosition(0);
        try {
            out.write(buffer.getAll());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final int BUFFER_SIZE = 4096;

    public void write(ReturnMessage protoState,BBuffer buffer){
        buffer.setPosition(0);
        buffer.truncate(0);
        try {
            protoState.write(buffer);
            out.write(buffer.getAll());
            out.flush();
            System.out.println("[SERVER][PROXY] Send: "+protoState.getClass().getSimpleName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public List<ReturnMessage> read(ProtoState protoState){
        var bb = new BBuffer();
        var be = new BytesEvent(null,null,bb);
        var lastPos=0;
        while(!protoState.canRunEvent(be)){
            bb.setPosition(lastPos);
            read(bb);
            lastPos = bb.getPosition();
            bb.setPosition(0);
        }
        var returnMessage = new ArrayList<ReturnMessage>();
        Iterator<ProtoStep> it = protoState.executeEvent(be);
        while ( it.hasNext() ) {
            returnMessage.add(it.next().run());
        }
        System.out.println("[SERVER][PROXY] Recv: "+protoState.getClass().getSimpleName());
        return returnMessage;
    }
    public void read(BBuffer buffer){
        try {
            var tmpba = new byte[BUFFER_SIZE];
            var counted = in.read(tmpba);

            if(counted<BUFFER_SIZE){
                var toCopy = new byte[counted];
                System.arraycopy(tmpba,0,toCopy,0,toCopy.length);
                buffer.write(toCopy);
            }else {
                buffer.write(tmpba);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {

        }
    }
}
