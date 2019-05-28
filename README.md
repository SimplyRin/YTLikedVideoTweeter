# YTLikedVideoTweeter
YouTube で高評価した動画を Twitter にツイートするやつ

Linux などで使用する場合、先に認証を Windows 環境で済ましておいたほうがセットアップが楽です()

- Config.yaml
```Yaml
# 高評価リストを何分単位で確認するかを設定する
Interval: 5
Data:
  # 以下のサイト(※1) にアクセスしてリダイレクトされた以下の部分(※2) をコピーして以下に貼り付け
  #
  # ※1: https://www.youtube.com/my_liked_videos
  # ※2: https://www.youtube.com/playlist?list=<この部分>
  Playlist-ID: LLoLLqIL88jUpp6Ned52u7mA
```

# Requiresments
- Java 8+
- Twitter Consumer key & Consumer token
- Google API client_secrets.json

# Download
- [YTLikedVideoTweeter-v1.0.jar](https://github.com/SimplyRin/YTLikedVideoTweeter/releases/1.0/YTLikedVideoTweeter-v1.0.jar)

# Open Source License
**・[Config | Apache License 2.0](https://github.com/SimplyRin/Config/blob/master/LICENSE.md)**

**・[HttpClient | Apache License 2.0](https://github.com/SimplyRin/HttpClient/blob/master/LICENSE.md)**

**・[RinStream | MIT License](https://github.com/SimplyRin/RinStream/blob/master/LICENSE.md)**

**・[BungeeCord (Config API) | BSD 3-Clause "New" or "Revised" License](https://github.com/SpigotMC/BungeeCord/blob/master/LICENSE)**

**・[Twitter4J | Apache License 2.0](https://github.com/Twitter4J/Twitter4J/blob/master/LICENSE.txt)**

**・[google-api-java-client | Apache License 2.0](https://github.com/googleapis/google-api-java-client/blob/master/LICENSE)**

**・[google-http-java-client | Apache License 2.0](https://github.com/googleapis/google-http-java-client/blob/master/LICENSE)**

**・[guava | Apache License 2.0](https://github.com/google/guava/blob/master/COPYING)**
