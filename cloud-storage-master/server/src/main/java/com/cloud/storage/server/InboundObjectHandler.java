package com.cloud.storage.server;

import com.cloud.storage.common.CommandMessage;
import com.cloud.storage.common.FileMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class InboundObjectHandler extends ChannelInboundHandlerAdapter {

    private String userLogin;
    private String userDir;
    private ConcurrentHashMap<String, ServerFileReceiver> receiveQueue;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null)
                return;

            if (msg instanceof CommandMessage) {
                String command = ((CommandMessage) msg).getCommand();

                if (command.equals(CommandMessage.GET_FILE_LIST)) {
                    ctx.write(new CommandMessage(CommandMessage.GET_FILE_LIST, FilesHandler.listDirectory(userDir), FilesHandler.getAvailableSize(userDir)));
                    ctx.flush();
                }

                if (command.equals(CommandMessage.RENAME_FILE_REQUEST)) {
                    if (FilesHandler.renameFile(userDir, ((CommandMessage) msg).getLogin(), ((CommandMessage) msg).getPassword())) {
                        ctx.write(new CommandMessage(CommandMessage.GET_FILE_LIST, FilesHandler.listDirectory(userDir), FilesHandler.getAvailableSize(userDir)));
                        ctx.write(new CommandMessage(CommandMessage.MESSAGE, "File renamed: " + ((CommandMessage) msg).getLogin() + "->" + ((CommandMessage) msg).getPassword()));
                        ctx.flush();
                    } else {
                        ctx.write(new CommandMessage(CommandMessage.MESSAGE, "Cannot rename file: " + ((CommandMessage) msg).getLogin()));
                        ctx.flush();
                    }
                }

                if (command.equals(CommandMessage.DELETE_FILE_REQUEST)) {
                    if (FilesHandler.deleteFile(userDir, ((CommandMessage) msg).getText())) {
                        ctx.write(new CommandMessage(CommandMessage.GET_FILE_LIST, FilesHandler.listDirectory(userDir), FilesHandler.getAvailableSize(userDir)));
                        ctx.write(new CommandMessage(CommandMessage.MESSAGE, "File deleted: " + ((CommandMessage) msg).getText()));
                        ctx.flush();

                    } else {
                        ctx.write(new CommandMessage(CommandMessage.MESSAGE, "Cannot delete file: " + ((CommandMessage) msg).getText()) + " Try again later.");
                        ctx.flush();
                    }
                }

                if (command.equals(CommandMessage.SEND_FILE_REQUEST)) {
                    if (FilesHandler.isFileExist(userDir, ((CommandMessage) msg).getFileName())) {
                        ctx.write(new CommandMessage(CommandMessage.SEND_FILE_DECLINE_EXIST, ((CommandMessage) msg).getFileName()));
                        ctx.flush();
                    } else if (((CommandMessage) msg).getFileSize() >= FilesHandler.getAvailableSize(userDir)) {
                        ctx.write(new CommandMessage(CommandMessage.SEND_FILE_DECLINE_SPACE, ((CommandMessage) msg).getFileName()));
                        ctx.flush();
                    } else {
                        receiveQueue.put(((CommandMessage) msg).getFileName(), new ServerFileReceiver(userDir, ((CommandMessage) msg).getFileName(), ((CommandMessage) msg).getFileSize()));
                        ctx.write(new CommandMessage(CommandMessage.SEND_FILE_CONFIRM, ((CommandMessage) msg).getFileName(), ((CommandMessage) msg).getFileSize()));
                        ctx.write(new CommandMessage(CommandMessage.GET_FILE_LIST, FilesHandler.listDirectory(userDir), FilesHandler.getAvailableSize(userDir)));
                        ctx.flush();
                    }
                }

                if (command.equals(CommandMessage.RECEIVE_FILE_REQUEST)) {
                    if(FilesHandler.isFileExist(userDir, ((CommandMessage) msg).getFileName())) {
                        ctx.write(new CommandMessage(CommandMessage.RECEIVE_FILE_CONFIRM, ((CommandMessage) msg).getFileName(), FilesHandler.getFileLength(userDir, ((CommandMessage) msg).getFileName())));
                        ctx.flush();
                    } else {
                        ctx.write(new CommandMessage(CommandMessage.RECEIVE_FILE_DECLINE, ((CommandMessage) msg).getFileName()));
                        ctx.flush();
                    }
                }

                if (command.equals(CommandMessage.RECEIVE_FILE_CONFIRM)) {
                    (new ServerFileSender(new File(userDir + "\\" + ((CommandMessage) msg).getText()))).sendFile(ctx);
                }
            }

            if (msg instanceof FileMessage) {
                ServerFileReceiver fr = receiveQueue.get(((FileMessage) msg).getName());
                if (fr != null) {
                    boolean isFinished = fr.receiveFile((FileMessage) msg);

                    if (isFinished) {
                        receiveQueue.remove(((FileMessage) msg).getName());
                        ctx.write(new CommandMessage(CommandMessage.MESSAGE, "File received: " + ((FileMessage) msg).getName()));
                        ctx.write(new CommandMessage(CommandMessage.GET_FILE_LIST, FilesHandler.listDirectory(userDir), FilesHandler.getAvailableSize(userDir)));
                        ctx.flush();
                    }
                }
            }

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //ctx.flush();
        ctx.close();
    }

    public InboundObjectHandler(String userLogin) {
        this.userLogin = userLogin;
        this.receiveQueue = new ConcurrentHashMap<>();
        userDir = SQLHandler.getUserFolderByLogin(userLogin);
        System.out.println("InboundObjectHandler  init. User: " + userLogin + " Dir: " + userDir);
    }
}
