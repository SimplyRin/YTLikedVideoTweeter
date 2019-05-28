package net.simplyrin.ytlikedvideotweeter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.FileCredentialStore;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.common.collect.Lists;

import net.md_5.bungee.config.Configuration;
import net.simplyrin.config.Config;
import net.simplyrin.rinstream.RinStream;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Created by SimplyRin on 2019/05/27.
 *
 * Copyright (c) 2019 SimplyRin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class Main {

	public static void main(String[] args) {
		new Main().run();
	}

	private File file;
	private File cache;

	private Configuration config;

	private Twitter twitter;

	private final static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private final static JsonFactory JSON_FACTORY = new JacksonFactory();

	public void run() {
		new RinStream();

		System.out.println("読み込み中...。");

		this.cache = new File("cache");
		this.cache.mkdir();

		this.file = new File("config.yaml");
		if (!this.file.exists()) {
			try {
				this.file.createNewFile();
			} catch (IOException e) {
			}

			Configuration config = Config.getConfig(this.file);

			config.set("YouTube.Update-Interval", 5);
			config.set("YouTube.Playlist-ID", "INPUT_YOUR_PLAYLIST_ID");

			config.set("Twitter.Status", "I liked @YouTube video %%url%%\n%%title%%");

			Config.saveConfig(config, this.file);

			System.out.println("config.yaml に必要な情報を入力してください。");
			System.exit(0);
		}

		File twitter = new File("twitter.yaml");
		if (!twitter.exists()) {
			try {
				twitter.createNewFile();
			} catch (IOException e) {
			}

			Configuration config = Config.getConfig(twitter);

			config.set("Consumer.Key", "KEY");
			config.set("Consumer.Secret", "SECRET");
			config.set("Access.Token", "TOKEN");
			config.set("Access.Secret", "SECRET");

			Config.saveConfig(config, twitter);
		}

		Configuration twitterConfig = Config.getConfig(twitter);

		if (twitterConfig.getString("Consumer.Key").equals("KEY") && twitterConfig.getString("Consumer.Secret").equals("SECRET")) {
			System.out.println("Consumer.Key と Consumer.Secret を twitter.yaml に入力してください！");
			System.exit(0);
		}

		this.twitter = TwitterFactory.getSingleton();
		this.twitter.setOAuthConsumer(twitterConfig.getString("Consumer.Key"), twitterConfig.getString("Consumer.Secret"));

		if (twitterConfig.getString("Access.Token").equals("TOKEN") && twitterConfig.getString("Access.Secret").equals("SECRET")) {
			RequestToken requestToken;
			try {
				requestToken = this.twitter.getOAuthRequestToken();
			} catch (TwitterException e) {
				return;
			}

			System.out.println("アカウント認証URL: " + requestToken.getAuthorizationURL());
			Scanner scanner = new Scanner(System.in);
			System.out.print("Twitter から提供されたピンを入力してください: ");

			AccessToken accessToken = null;
			try {
				accessToken = this.twitter.getOAuthAccessToken(requestToken, scanner.nextLine());
			} catch (TwitterException e) {
				e.printStackTrace();
				System.exit(0);
			}

			scanner.close();

			twitterConfig.set("Access.Token", accessToken.getToken());
			twitterConfig.set("Access.Secret", accessToken.getTokenSecret());

			Config.saveConfig(twitterConfig, twitter);
		}

		this.twitter.setOAuthAccessToken(new AccessToken(twitterConfig.getString("Access.Token"), twitterConfig.getString("Access.Secret")));
		try {
			User user = this.twitter.verifyCredentials();
			System.out.println("認証されたアカウント: @" + user.getScreenName());
		} catch (TwitterException e) {
			e.printStackTrace();
			return;
		}

		this.config = Config.getConfig(this.file);

		File clientSecrets = new File("client_secrets.json");
		if (!clientSecrets.exists()) {
			System.out.println("以下の API サイトから OAuth クライアント ID を作成し 'client_secrets.json' として Jar ファイル実行と同じディレクトリ内に保存してください。");
			System.out.println("OAuth クライアント ID の作成: https://console.developers.google.com/apis/credentials/oauthclient");
			System.exit(0);
		}

		System.out.println("読み込みが完了しました。高評価した動画を取得し、自動的にツイートします。");

		this.task();
	}

	private boolean firstCheck = true;

	public void task() {
		while (true) {
			this.config = Config.getConfig(this.file);

			List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");

			try {
				Credential credential = this.authorize(scopes);

				YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName("youtube-cmdline-playlistupdates-sample").build();

				YouTube.PlaylistItems.List request = youtube.playlistItems().list("snippet");
				request.setPlaylistId(this.config.getString("YouTube.Playlist-ID"));
				PlaylistItemListResponse channelResult = request.execute();
				List<PlaylistItem> playlistItems = channelResult.getItems();

				if (playlistItems != null) {
					for (PlaylistItem playlistItem : playlistItems) {
						try {
							String id = "";
							for (Thumbnail thumbnail : playlistItem.getSnippet().getThumbnails().values()) {
								id = thumbnail.getUrl().replace("https://i.ytimg.com/vi/", "");
								id = id.split("/")[0];
							}

							File cacheFile = new File(this.cache, id);
							if (!cacheFile.exists()) {
								cacheFile.createNewFile();

								if (!this.firstCheck) {
									System.out.println("新しい高評価した動画を検出しました。ツイートします...。");

									String status = this.config.getString("Twitter.Status");

									status = status.replace("%%url%%", "https://youtu.be/" + id);
									status = status.replace("%%title%%", playlistItem.getSnippet().getTitle());

									this.twitter.updateStatus(status);
									System.out.println("ツイートしました: " + playlistItem.getSnippet().getTitle() + "(" + id + ")");
								}
							}
						} catch (Exception e) {
						}
					}
				}
			} catch (GoogleJsonResponseException e) {
				e.printStackTrace();
				System.err.println("There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
			} catch (Throwable t) {
				t.printStackTrace();
			}

			this.firstCheck = false;

			try {
				TimeUnit.MINUTES.sleep(this.config.getInt("YouTube.Update-Interval"));
			} catch (Exception e) {
			}
		}
	}

	private Credential authorize(List<String> scopes) throws Exception {
		File file = new File("client_secrets.json");

		GoogleClientSecrets googleClientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new FileInputStream(file));

		if (googleClientSecrets.getDetails().getClientId().startsWith("Enter") || googleClientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
			System.out.println("Enter Client ID and Secret from https://code.google.com/apis/console/?api=youtube into client_secrets.json");
			System.exit(0);
		}

		FileCredentialStore fileCredentialStore = new FileCredentialStore(new File("credentials.json"), JSON_FACTORY);
		GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, googleClientSecrets, scopes).setCredentialStore(fileCredentialStore).build();
		LocalServerReceiver localServerReceiver = new LocalServerReceiver.Builder().setPort(8080).build();

	    return new AuthorizationCodeInstalledApp(googleAuthorizationCodeFlow, localServerReceiver).authorize("user");
	}

}
