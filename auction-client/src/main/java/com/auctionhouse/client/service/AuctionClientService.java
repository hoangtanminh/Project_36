//service
package com.auctionhouse.client.service;

import com.auction.shared.enums.CommandType;
import com.auction.shared.enums.ResponseStatus;
import com.auction.model.Auction;
import com.auction.shared.dto.DashboardView;
import com.auction.model.User;
import com.auction.shared.protocol.AuctionActionRequest;
import com.auction.shared.protocol.AuctionSelectionRequest;
import com.auction.shared.protocol.AuctionStatusChangeRequest;
import com.auction.shared.protocol.AuctionSubscriptionRequest;
import com.auction.shared.protocol.BidRequest;
import com.auction.shared.protocol.ClientRequest;
import com.auction.shared.protocol.CreateAuctionRequest;
import com.auction.shared.protocol.DashboardRequest;
import com.auction.shared.protocol.LoginRequest;
import com.auction.shared.protocol.LogoutRequest;
import com.auction.shared.protocol.RegisterRequest;
import com.auction.shared.protocol.ServerResponse;
import com.auction.shared.protocol.UpdateAuctionRequest;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public final class AuctionClientService implements Closeable {
    private final String host;      //ip sever
    private final int port;     // cổng trong sever
    //Queue chứa các response từ sever chờ xử lí
    private final BlockingQueue<ServerResponse<?>> responses = new LinkedBlockingQueue<>();
    private volatile Consumer<ServerResponse<?>> eventListener = response -> {
    };  // được gọi khi có response từ sever để thông báo khi nhận được dữ liệu
    private Socket socket;   //kết nối TCP với socket  (TCP là giao thức giúp 2 máy gửi dữ liệu cho nhau)
    private ObjectOutputStream outputStream;    //dữ liệu nhận về
    private ObjectInputStream inputStream;  //dữ liệu gửi đi
    private CompletableFuture<Void> listenerLoop;   //đọc dữ liệu liên tục

    public AuctionClientService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    //nếu respone từ sever gửi về là null thì hàm rỗng
    public void setEventListener(Consumer<ServerResponse<?>> eventListener) {
        this.eventListener = eventListener == null ? response -> {
        } : eventListener;
    }

    // uI gọi hàm login , client tạo request gửi cho sever check , sever trả về kết quả cho client
    public User login(String username, String password) {
        return expectPayload(sendAndAwait(new ClientRequest<>(CommandType.LOGIN, new LoginRequest(username, password))), User.class);
    }

    public User register(RegisterRequest request) {
        return expectPayload(sendAndAwait(new ClientRequest<>(CommandType.REGISTER, request)), User.class);
    }

    public DashboardView loadDashboard(String username) {
        return expectPayload(sendAndAwait(new ClientRequest<>(CommandType.LOAD_DASHBOARD, new DashboardRequest(username))), DashboardView.class);
    }

    public Auction loadAuction(long auctionId) {
        return expectPayload(sendAndAwait(new ClientRequest<>(CommandType.LOAD_AUCTION_DETAILS, new AuctionSelectionRequest(auctionId))), Auction.class);
    }

    public Auction subscribe(long auctionId, String username) {
        return expectPayload(sendAndAwait(
                new ClientRequest<>(CommandType.SUBSCRIBE_AUCTION, new AuctionSubscriptionRequest(auctionId, username))),
                Auction.class);
    }

    public Auction placeBid(BidRequest request) {
        return expectPayload(sendAndAwait(new ClientRequest<>(CommandType.PLACE_BID, request)), Auction.class);
    }

    public Auction createAuction(CreateAuctionRequest request) {
        return expectPayload(sendAndAwait(new ClientRequest<>(CommandType.CREATE_AUCTION, request)), Auction.class);
    }

    public Auction updateAuction(UpdateAuctionRequest request) {
        return expectPayload(sendAndAwait(new ClientRequest<>(CommandType.UPDATE_AUCTION, request)), Auction.class);
    }

    public void deleteAuction(AuctionActionRequest request) {
        sendAndAwait(new ClientRequest<>(CommandType.DELETE_AUCTION, request));
    }

    public Auction changeAuctionStatus(AuctionStatusChangeRequest request) {
        return expectPayload(sendAndAwait(new ClientRequest<>(CommandType.CHANGE_AUCTION_STATUS, request)), Auction.class);
    }

    private synchronized ServerResponse<?> sendAndAwait(ClientRequest<?> request) {
        ensureConnected();  // đảm bảo đã kết nối
        try {
            outputStream.writeObject(request);  //tạo object
            outputStream.flush();   //đẩy đi luộn
            outputStream.reset();   // reset để tránh lỗi
            ServerResponse<?> response = responses.take();
            if (response.getStatus() == ResponseStatus.ERROR) {
                throw new IllegalStateException(response.getMessage());     //nếu sever báo lỗi thì hiện exception
            }
            return response; //trả về kết qả
        } catch (IOException exception) {   // bắt lỗi network
            throw new IllegalStateException("Network error: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for server response.", exception);
        }
    }

    // hàm để đảm bảo đã kết nối với sever
    private void ensureConnected() {
        if (socket != null && socket.isConnected() && !socket.isClosed()) { //đã có socket , kết nối , chưa đóng
            return;
        }
        //tạo kết nối mới
        try {
            socket = new Socket(host, port);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());
            listenerLoop = CompletableFuture.runAsync(this::listenForResponses);
        //bắt lỗi không connect được
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot connect to auction server at " + host + ":" + port, exception);
        }
    }

    //hàm nghe lén, chạy nền , đọc dữ liệu liên tục
    private void listenForResponses() {
        try {
            while (socket != null && !socket.isClosed()) {  // chỉ dừng khi socket null hoặc bị đóng
                Object payload = inputStream.readObject();//đọc giữ liệu
                if (!(payload instanceof ServerResponse<?> response)) { // nếu không phải dữ liệu cùa sever bỏ qa
                    continue;
                }
                // nếu là response real-time thì xử lí luôn , không thì để sendAndAwait() lấy ra
                if (response.getStatus() == ResponseStatus.EVENT) {
                    eventListener.accept(response);
                } else {
                    responses.offer(response);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T expectPayload(ServerResponse<?> response, Class<T> type) {
        Object payload = response.getPayload(); // lấy dữ liệu trong ResponseSever
        if (!type.isInstance(payload)) {
            throw new IllegalStateException("Unexpected response payload: " + payload);// nếu sai kiểu thì hiện lỗi
        }
        return (T) payload; // đúng kiểu thì ép lại  rồi trả về
    }

    @Override
    //hàm đóng kết nối của client với sever
    public synchronized void close() throws IOException {
        try {
            if (outputStream != null && socket != null && socket.isConnected() && !socket.isClosed()) {
                outputStream.writeObject(new ClientRequest<>(CommandType.LOGOUT, new LogoutRequest("client")));
                outputStream.flush();
            }
        } catch (Exception ignored) {
        }

        if (socket != null) {
            socket.close();
        }
    }
}

