//AuctionClientApp
package com.auctionhouse.client;

import com.auctionhouse.client.service.AuctionClientService;
import com.auctionhouse.client.view.AppCoordinator;
import javafx.application.Application;
import javafx.stage.Stage;

public final class AuctionClientApp extends Application {
    private AuctionClientService clientService;

    @Override
    public void start(Stage stage) throws Exception {
        // stage là cửa sổ app
        clientService = new AuctionClientService("localhost", 5050); // kết nối tới sever
        AppCoordinator coordinator = new AppCoordinator(stage, clientService);
        coordinator.showLogin();//hiển thị màn hình đầu tiên
    }

    @Override
    public void stop() throws Exception {
        //khi tắt app ngắt kết nối với sever
        if (clientService != null) {
            clientService.close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

