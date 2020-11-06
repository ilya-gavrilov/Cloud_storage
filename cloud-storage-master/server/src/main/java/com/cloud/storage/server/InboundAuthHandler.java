package com.cloud.storage.server;

import com.cloud.storage.common.CommandMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;

public class InboundAuthHandler extends ChannelInboundHandlerAdapter {

    boolean isClientAuthorized;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("New client tries to connect");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (isClientAuthorized) {
            ctx.fireChannelRead(msg);
            return;

        } else {
            if (msg instanceof CommandMessage) {
                String command = ((CommandMessage) msg).getCommand();

                if (command.equals(CommandMessage.AUTH_REQUEST)) {
                    if (checkAuth(((CommandMessage) msg).getLogin(), ((CommandMessage) msg).getPassword())) {
                        isClientAuthorized = true;
                        ctx.pipeline().addLast(new InboundObjectHandler(((CommandMessage) msg).getLogin()));
                        ctx.write(new CommandMessage(CommandMessage.AUTH_CONFIRM));
                        ctx.flush();
                        System.out.println("client authorized with: " + ((CommandMessage) msg).getLogin() + " " + ((CommandMessage) msg).getPassword());

                    } else {
                        System.out.println("bad login password");
                        ctx.write(new CommandMessage(CommandMessage.AUTH_DECLINE));
                        ctx.flush();
                    }
                } else if (command.equals(CommandMessage.REGISTER_NEW_USER)) {
                    if (tryToRegisterUser(((CommandMessage) msg).getLogin(), ((CommandMessage) msg).getPassword())) {
                        isClientAuthorized = true;
                        ctx.pipeline().addLast(new InboundObjectHandler(((CommandMessage) msg).getLogin()));
                        ctx.write(new CommandMessage(CommandMessage.REGISTER_CONFIRM, "New user with login " + ((CommandMessage) msg).getLogin() + " successfully registered!"));
                        ctx.write(new CommandMessage(CommandMessage.AUTH_CONFIRM));
                        ctx.flush();
                        System.out.println("register ok");
                    } else {
                        ctx.write(new CommandMessage(CommandMessage.REGISTER_DECLINE));
                        ctx.flush();
                    }
                }
            } else {
                System.out.println("object not a message");
                ctx.flush();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private boolean tryToRegisterUser(String login, String password) {
        String userDirPath = SettingsMgmt.ROOT_FOLDER + login;

        if (SQLHandler.tryToRegister(login, password, userDirPath)) {
            if (new File(userDirPath).mkdir()) {
                return true;
            }
        }
        return false;
    }

    private boolean checkAuth(String login, String password) {
        return (SQLHandler.getUserFolderByLoginAndPassword(login, password) != null);
    }
}
