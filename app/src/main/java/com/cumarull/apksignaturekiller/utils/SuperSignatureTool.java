package com.cumarull.apksignaturekiller.utils;

import android.content.Context;

import com.cumarull.apksignaturekiller.R;

import org.jetbrains.annotations.NotNull;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;

import bin.util.StreamUtil;
import bin.xml.decode.AXmlDecoder;
import bin.xml.decode.AXmlResourceParser;
import bin.xml.decode.XmlPullParser;
import bin.zip.ZipEntry;
import bin.zip.ZipFile;
import bin.zip.ZipOutputStream;

public class SuperSignatureTool {
    private Context mContext;

    private boolean customApplication = false;
    private String customApplicationName;
    private String packageName;

    private String outApk;
    private String srcApk;
    private String tempApk;

    public SuperSignatureTool(Context context) {
        mContext = context;
    }

    public void setPath(String input, String output) {
        srcApk = input;
        outApk = output;
        tempApk = new File(srcApk).getParentFile().toString() + "/.temp";
    }

    public void Kill() throws IOException {
        new File(outApk).delete();
        System.out.println("Чтение APK:" + srcApk);
        try (ZipFile zipFile = new ZipFile(srcApk);) {
            byte[] apk = StreamUtil.readBytes(new FileInputStream(srcApk));

            System.out.println("  -- Обработка AndroidManifest.xml");
            ZipEntry manifestEntry = zipFile.getEntry("AndroidManifest.xml");
            byte[] manifestData = parseManifest(zipFile.getInputStream(manifestEntry));

            System.out.println("  -- Обработка classes.dex");
            byte[] dexData;
            try {
                dexData = processDex();
            } catch (Throwable th) {
                throw new IOException("Failed to build classes.dex: " + th.getMessage(), th);
            }

            System.out.println("\nЗапись в APK:" + outApk);
            try (ZipOutputStream zos = new ZipOutputStream(new File(outApk))) {
                zos.putNextEntry("AndroidManifest.xml");
                zos.write(manifestData);
                zos.closeEntry();

                zos.putNextEntry("classes.dex");
                zos.write(dexData);
                zos.closeEntry();

                zos.putNextEntry("assets/ysh/hook.apk");
                zos.write(apk);
                zos.closeEntry();

                Enumeration<ZipEntry> enumeration = zipFile.getEntries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry ze = enumeration.nextElement();
                    if (ze.getName().equals("AndroidManifest.xml")
                            || ze.getName().equals("classes.dex")
                            || ze.getName().startsWith("META-INF/"))
                        continue;
                    zos.copyZipEntry(ze, zipFile);
                }
            }
            new File(tempApk).delete();
        } catch (Throwable th2) {
            throw new IOException("Kill failed for " + srcApk + ": " + th2.getMessage(), th2);
        }
    }

    private byte @NotNull [] processDex() throws Exception {
        String origDexPath = mContext.getFilesDir().getAbsolutePath() + "/classesO.dex";
        FileUtils.extractFile("classes.dex", srcApk, origDexPath);

        DexBackedDexFile origDex;
        try (InputStream orig = new FileInputStream(origDexPath)) {
            origDex = DexBackedDexFile.fromInputStream(Opcodes.forApi(20), new BufferedInputStream(orig));
        }

        DexBackedDexFile hookDex;
        try (InputStream hook = mContext.getResources().openRawResource(R.raw.super_hook)) {
            hookDex = DexBackedDexFile.fromInputStream(Opcodes.forApi(20), new BufferedInputStream(hook));
        }

        DexPool dexPool = new DexPool(Opcodes.forApi(20));
        try (InputStream fis = mContext.getResources().openRawResource(R.raw.super_hook_app)) {
            String src = new String(StreamUtil.readBytes(fis), StandardCharsets.UTF_8);
            if (customApplication) {
                if (customApplicationName.startsWith(".")) {
                    if (packageName == null)
                        throw new NullPointerException("Package name is null.");
                    customApplicationName = packageName + customApplicationName;
                }
                customApplicationName = "L" + customApplicationName.replace('.', '/') + ";";
                src = src.replace("Landroid/app/Application;", customApplicationName);
            }
            DexBuilder smaliBuilder = new DexBuilder(Opcodes.forApi(20));
            FileUtils.assembleSmaliString(src, smaliBuilder);
            MemoryDataStore smaliStore = new MemoryDataStore();
            smaliBuilder.writeTo(smaliStore);
            try (InputStream in = smaliStore.readAt(0)) {
                DexBackedDexFile smaliDex = DexBackedDexFile.fromInputStream(Opcodes.forApi(20), new BufferedInputStream(in));
                for (ClassDef c : smaliDex.getClasses()) {
                    dexPool.internClass(c);
                }
            }
        }
        for (ClassDef c : origDex.getClasses()) {
            dexPool.internClass(c);
        }
        for (ClassDef c : hookDex.getClasses()) {
            dexPool.internClass(c);
        }
        MemoryDataStore store = new MemoryDataStore();
        dexPool.writeTo(store);
        return store.getData();
    }

    private byte @NotNull [] parseManifest(InputStream is) throws IOException {
        AXmlDecoder axml = AXmlDecoder.decode(is);
        AXmlResourceParser parser = new AXmlResourceParser();
        parser.open(new ByteArrayInputStream(axml.getData()), axml.mTableStrings);
        boolean success = false;

        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG)
                continue;
            if (parser.getName().equals("manifest")) {
                int size = parser.getAttributeCount();
                for (int i = 0; i < size; ++i) {
                    if (parser.getAttributeName(i).equals("package")) {
                        packageName = parser.getAttributeValue(i);
                    }
                }
            } else if (parser.getName().equals("application")) {
                int size = parser.getAttributeCount();
                for (int i = 0; i < size; ++i) {
                    if (parser.getAttributeNameResource(i) == 0x01010003) {
                        customApplication = true;
                        customApplicationName = parser.getAttributeValue(i);
                        int index = axml.mTableStrings.getSize();
                        byte[] data = axml.getData();
                        int off = parser.currentAttributeStart + 20 * i;
                        off += 8;
                        FileUtils.writeInt(data, off, index);
                        off += 8;
                        FileUtils.writeInt(data, off, index);
                    }
                }
                if (!customApplication) {
                    int off = parser.currentAttributeStart;
                    byte[] data = axml.getData();
                    byte[] newData = new byte[data.length + 20];
                    System.arraycopy(data, 0, newData, 0, off);
                    System.arraycopy(data, off, newData, off + 20, data.length - off);

                    // chunkSize
                    int chunkSize = FileUtils.readInt(newData, off - 32);
                    FileUtils.writeInt(newData, off - 32, chunkSize + 20);
                    // attributeCount
                    FileUtils.writeInt(newData, off - 8, size + 1);

                    int idIndex = parser.findResourceID(0x01010003);
                    if (idIndex == -1)
                        throw new IOException("idIndex == -1");

                    boolean isMax = true;
                    for (int i = 0; i < size; ++i) {
                        int id = parser.getAttributeNameResource(i);
                        if (id > 0x01010003) {
                            isMax = false;
                            if (i != 0) {
                                System.arraycopy(newData, off + 20, newData, off, 20 * i);
                                off += 20 * i;
                            }
                            break;
                        }
                    }
                    if (isMax) {
                        System.arraycopy(newData, off + 20, newData, off, 20 * size);
                        off += 20 * size;
                    }

                    FileUtils.writeInt(newData, off, axml.mTableStrings.find("http://schemas.android.com/apk/res/android"));
                    FileUtils.writeInt(newData, off + 4, idIndex);
                    FileUtils.writeInt(newData, off + 8, axml.mTableStrings.getSize());
                    FileUtils.writeInt(newData, off + 12, 0x03000008);
                    FileUtils.writeInt(newData, off + 16, axml.mTableStrings.getSize());
                    axml.setData(newData);
                }
                success = true;
                break;
            }
        }
        if (!success)
            throw new IOException();
        ArrayList<String> list = new ArrayList<>(axml.mTableStrings.getSize());
        axml.mTableStrings.getStrings(list);
        list.add("com.ysh.hook.App");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        axml.write(list, baos);
        return baos.toByteArray();
    }
}