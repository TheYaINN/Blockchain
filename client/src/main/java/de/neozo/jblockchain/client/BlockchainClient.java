package de.neozo.jblockchain.client;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.neozo.jblockchain.common.SignatureUtils;
import de.neozo.jblockchain.common.domain.Address;
import de.neozo.jblockchain.common.domain.Block;
import de.neozo.jblockchain.common.domain.Transaction;
import org.apache.commons.codec.binary.Base64;
import org.springframework.web.client.RestTemplate;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;
import java.util.zip.GZIPInputStream;


/**
 * Simple class to help building REST calls for jBlockchain.
 * Just run it in command line for instructions on how to use it.
 * <p>
 * Functions include:
 * - Generate Private/Public-Key
 * - Publish a new Address
 * - Publish a new Transaction
 */
public class BlockchainClient {

    private final String serverPort;
    private final String storePath;
    private final String serverAddress;
    private final Boolean sslEnabled;

    private final String SSL_CONNECTION = "https";
    private final String CONNECTION = "http";

    private final String url;
    private final Path pub;
    private final Path priv;
    private Address address;


    public static void main(String args[]) throws Exception {
        BlockchainClient client = new BlockchainClient("8080", "blockchain", "localhost", false);
    }

    BlockchainClient(String serverPort, String storePath, String serverAddress, Boolean sslEnabled) {
        this.serverPort = serverPort;
        this.storePath = storePath;
        this.serverAddress = serverAddress;
        this.sslEnabled = sslEnabled;
        url = buildUrl();
        pub = Paths.get("key.pub");
        priv = Paths.get("key.priv");

        try {
            generateKeyPair();
        } catch (NoSuchProviderException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame();
        frame.setSize(400, 400);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel top = new JPanel(new FlowLayout());
        frame.add(top, BorderLayout.NORTH);

        JButton startMiner = new JButton("Start");
        startMiner.addActionListener(e -> start());
        top.add(startMiner);

        JButton stopMiner = new JButton("Stop");
        stopMiner.addActionListener(e -> stop());
        top.add(stopMiner);

        JPanel center = new JPanel(new FlowLayout());
        frame.add(center, BorderLayout.CENTER);

        JButton pubAddress = new JButton("Publish Address");
        pubAddress.addActionListener(e -> publishAddress(url, pub, "Bengt"));
        center.add(pubAddress);

        JButton transaction = new JButton("Publish Transaction");
        transaction.addActionListener(e -> publishTransaction(url, priv, "Some text", address.getHash()));
        center.add(transaction);

        JButton blockchain = new JButton("get Blockchain");
        blockchain.addActionListener(e -> {
            try {
                getBlockchain();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        center.add(blockchain);

        frame.setVisible(true);
    }

    public void getBlockchain() throws IOException {
        String protocol = sslEnabled ? "https" : "http";
        HttpURLConnection con = (HttpURLConnection) new URL(String.format("%s/block", buildUrl())).openConnection();
        con.setRequestProperty("Accept-Encoding", "gzip");
        InputStream inp;
        if ("gzip".equals(con.getContentEncoding())) {
            inp = new GZIPInputStream(con.getInputStream());
        } else {
            inp = con.getInputStream();
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        TypeReference<List<Block>> typeReference = new TypeReference<List<Block>>() {
        };
        System.out.println(mapper.readValue(inp, typeReference));
    }

    public void start() {
        callService("/block/start-miner");
    }

    public void stop() {
        callService("/block/stop-miner");
    }

    private void generateKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException, IOException {
        KeyPair keyPair = SignatureUtils.generateKeyPair();
        Files.write(Paths.get("key.priv"), keyPair.getPrivate().getEncoded());
        Files.write(Paths.get("key.pub"), keyPair.getPublic().getEncoded());
    }

    public void publishAddress(String node, Path publicKey, String name) {
        RestTemplate restTemplate = new RestTemplate();
        address = new Address(name, getKey(publicKey));
        restTemplate.put(node + "/address?publish=true", address);
        System.out.println("Hash of new address: " + Base64.encodeBase64String(address.getHash()));
    }

    public void publishTransaction(String node, Path privateKey, String text, byte[] senderHash) {
        RestTemplate restTemplate = new RestTemplate();
        byte[] signature = new byte[0];
        try {
            signature = SignatureUtils.sign(text.getBytes(), getKey(privateKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Transaction transaction = new Transaction(text, senderHash, signature);
        restTemplate.put(node + "/transaction?publish=true", transaction);
        System.out.println("Hash of new transaction: " + Base64.encodeBase64String(transaction.getHash()));
    }

    private byte[] getKey(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[]{};
    }


    private void callService(String path) {
        RestTemplate template = new RestTemplate();
        template.getForEntity(String.format("%s%s", buildUrl(), path), String.class);
    }

    private String buildUrl() {
        return getProtocol() + "://" + serverAddress + ":" + serverPort;
    }


    private String getProtocol() {
        return sslEnabled ? SSL_CONNECTION : CONNECTION;
    }
}
