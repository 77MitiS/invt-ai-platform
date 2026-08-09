package vip.mate.enterprise;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File-based data service for enterprise workspace modules.
 * Stores contract review and account intel data as JSON files
 * under ~/.invt/enterprise-data/.
 */
@Slf4j
@Service
public class EnterpriseDataService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path dataDir;
    private final Map<String, List<Map<String, Object>>> cache = new ConcurrentHashMap<>();

    public EnterpriseDataService() {
        this.dataDir = Paths.get(System.getProperty("user.home"), ".invt", "enterprise-data");
        try { Files.createDirectories(dataDir); } catch (IOException e) { log.warn("Cannot create enterprise data dir", e); }
    }

    // ==================== Contracts ====================

    public synchronized List<Map<String, Object>> getContracts() {
        return load("contracts.json", defaultContracts());
    }

    public synchronized Map<String, Object> saveContract(Map<String, Object> contract) {
        var list = new ArrayList<>(getContracts());
        if (contract.get("id") == null) contract.put("id", UUID.randomUUID().toString().substring(0, 8));
        // Replace existing or add
        var opt = list.stream().filter(c -> contract.get("id").equals(c.get("id"))).findFirst();
        if (opt.isPresent()) {
            int idx = list.indexOf(opt.get());
            list.set(idx, contract);
        } else {
            list.add(contract);
        }
        save("contracts.json", list);
        return contract;
    }

    public synchronized void deleteContract(String id) {
        var list = new ArrayList<>(getContracts());
        list.removeIf(c -> id.equals(c.get("id")));
        save("contracts.json", list);
    }

    // ==================== Accounts ====================

    public synchronized List<Map<String, Object>> getAccounts() {
        return load("accounts.json", defaultAccounts());
    }

    public synchronized Map<String, Object> saveAccount(Map<String, Object> account) {
        var list = new ArrayList<>(getAccounts());
        if (account.get("id") == null) account.put("id", UUID.randomUUID().toString().substring(0, 8));
        var opt = list.stream().filter(a -> account.get("id").equals(a.get("id"))).findFirst();
        if (opt.isPresent()) {
            int idx = list.indexOf(opt.get());
            list.set(idx, account);
        } else {
            list.add(account);
        }
        save("accounts.json", list);
        return account;
    }

    public synchronized void deleteAccount(String id) {
        var list = new ArrayList<>(getAccounts());
        list.removeIf(a -> id.equals(a.get("id")));
        save("accounts.json", list);
    }

    // ==================== File I/O ====================

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> load(String fileName, List<Map<String, Object>> defaults) {
        if (cache.containsKey(fileName)) return cache.get(fileName);
        Path file = dataDir.resolve(fileName);
        if (Files.exists(file)) {
            try {
                List<Map<String, Object>> data = mapper.readValue(file.toFile(),
                        new TypeReference<List<Map<String, Object>>>() {});
                cache.put(fileName, data);
                return data;
            } catch (IOException e) {
                log.warn("Failed to read {}, using defaults", fileName);
            }
        }
        save(fileName, defaults);
        cache.put(fileName, defaults);
        return defaults;
    }

    private void save(String fileName, List<Map<String, Object>> data) {
        cache.put(fileName, data);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(dataDir.resolve(fileName).toFile(), data);
        } catch (IOException e) {
            log.warn("Failed to save {}", fileName, e);
        }
    }

    // ==================== Default Data ====================

    private List<Map<String, Object>> defaultContracts() {
        return new ArrayList<>();
    }

    private List<Map<String, Object>> defaultAccounts() {
        return new ArrayList<>();
    }

    private static Map<String, Object> mapOf(String... kvs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length - 1; i += 2) m.put(kvs[i], kvs[i + 1]);
        return m;
    }
}
