# 🦅 Cardinal System Core

![Version](https://img.shields.io/badge/version-1.0.0--RELEASE-blue?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Architecture](https://img.shields.io/badge/Architecture-Modular%20%2F%20Microservice-green?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

**Cardinal System**, dinamik modül yükleme kapasitesine sahip, yüksek performanslı ve genişletilebilir bir backend yönetim platformudur. OSHI entegrasyonu ile gerçek zamanlı donanım izleme ve harici arayüzler (UI) için optimize edilmiş bir Socket API sunar.

---

## 📖 İçindekiler
* [🏗 Sistem Mimarisi](#-sistem-mimarisi)
* [🚀 Kurulum ve Yapılandırma](#-kurulum-ve-yapılandırma)
* [🧩 Modül Yönetim Sistemi](#-modül-yönetim-sistemi)
* [📊 Sistem İzleyici (Monitor)](#-sistem-izleyici-monitor)
* [📡 Ağ Protokolü (Socket API)](#-ağ-protokolü-socket-api)
* [⌨ CLI Komut Seti](#-cli-komut-seti)

---

## 🏗 Sistem Mimarisi

Cardinal, **Microkernel** tasarım desenini benimser. Çekirdek (Core) yalnızca hayati fonksiyonları barındırırken, tüm işlevsel özellikler bağımsız modüller üzerinden yürütülür.

| Bileşen | Görev |
| :--- | :--- |
| **Core (Main)** | Uygulama yaşam döngüsünü (Startup/Shutdown) yönetir. |
| **ModuleManager** | `.jar` tabanlı eklentileri çalışma zamanında yükler ve Event yönetimini sağlar. |
| **SystemMonitor** | Arka planda (Daemon Thread) donanım verilerini toplar. |
| **Socket API** | Toplanan verileri 5000 portu üzerinden dış dünyaya servis eder. |

---

## 🚀 Kurulum ve Yapılandırma

### Gereksinimler
* **Runtime:** JDK 21 veya üzeri.
* **Build Tool:** Maven.
* **Dependencies:** OSHI Core, JNA.

### Dizin Yapısı
Derleme sonrası oluşması gereken standart çalışma dizini:
```text
Cardinal/
├── Cardinal.jar           # Ana Uygulama
├── libs/                  # Bağımlılıklar (OSHI, JNA, vb.)
├── modules/               # Harici modüller (.jar)
│   ├── AuthModule.jar
│   └── LoggerModule.jar
├── logs/                  # Sistem log kayıtları
└── configs/               # Yapılandırma (YAML/JSON)
```

---
## 🧩 Modül Yönetim Sistemi
Cardinal, **Hot-Reload** mantığıyla sistem kapanmadan modül yükleyip boşaltabilir.

### Modül Geliştirme
Her modülün IModule arayüzünü implemente etmesi zorunludur:

```java
public class MyModule implements IModule {
    @Override
    public void onEnable() {
        // Modül aktif edildiğinde çalışacak mantık
        Logger.info("Module Active!");
    }

    @Override
    public void onDisable() {
        // Güvenli kapatma işlemleri
        Logger.info("Module Disabled!");
    }
}
```

--- 

## 📊 Sistem İzleyici (Monitor)
**SystemMonitor** sınıfı, ana akışı engellememek için **java.lang.Thread** sınıfını extend eder. Donanım verilerini OSHI kütüphanesi ile doğrudan işletim sistemi katmanından çeker.

* **CPU:** Tick tabanlı hassas ölçüm.
* **RAM:** Toplam/Kullanılan oran analizi.
* **AI Status:** Yapay zeka servisinden gelen sistem sağlık raporu (Stable/Critical).

  ---
  
## 📡 Ağ Protokolü (Socket API)
Harici arayüzlerin **(JavaFX, Web Panel, Mobile)** sisteme erişimi için optimize edilmiş bir **TCP** sunucusu barındırır.

* **Port:** 5000(varsayılan)
* **Protokol:** TCP/String Based
* **Format:** CPU_LOAD | RAM_USAGE | ACTIVE_USERS | SYSTEM_STATUS

```bash
Örnek Yanıt: 45.2|60.1|150|STABIL
```

```java
try (Socket socket = new Socket("127.0.0.1", 5000)) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    String data = reader.readLine(); 
    // Gelen veri parse edilerek UI elementlerine yansıtılır.
}
```
---

## ⌨ CLI Komut Seti
Konsol üzerinden sistemi yönetmek için aşağıdaki komutlar kullanılabilir:

| Komut | Açıklama |
| :--- | :--- |
| **Help** | Tüm kullanılabilir komutları listeler. |
| **modules** | `.jar` tabanlı eklentileri çalışma zamanında yükler ve Event yönetimini sağlar. |
| **monitor** | Arka planda (Daemon Thread) donanım verilerini toplar. |
| **ai-status** | Toplanan verileri 5000 portu üzerinden dış dünyaya servis eder. |
| **stop**    | |Sistemi ve modülleri güvenli şekilde kapatır. |

---

## 🛠 Geliştirici Notları
* Sistem **Singleton Design Pattern** üzerine inşa edilmiştir.
* **ModuleManager** veya **SystemMonitor** erişimleri için statik instance metodlarını kullanın.
* Olay tabanlı tetiklemeler için **Event-Driven Architecture** yapısını bozmamaya özen gösterin.
