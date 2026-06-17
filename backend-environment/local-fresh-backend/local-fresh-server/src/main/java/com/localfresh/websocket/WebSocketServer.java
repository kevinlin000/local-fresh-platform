package com.localfresh.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务
 */
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    private static Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 連接建立成功呼叫的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        log.info("客戶端：{}建立連接", sid);
        sessionMap.put(sid, session);
    }

    /**
     * 收到客戶端訊息後呼叫的方法
     *
     * @param message 客戶端傳送過來的訊息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到來自客戶端：{}的訊息:{}", sid, message);
    }

    /**
     * 連接关闭呼叫的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        log.info("連接斷開:{}", sid);
        sessionMap.remove(sid);
    }

    /**
     * 廣播
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                //伺服器向客戶端傳送訊息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                log.error("WebSocket 發送訊息失敗", e);
            }
        }
    }

}
