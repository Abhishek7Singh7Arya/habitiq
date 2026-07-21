import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  useColorScheme,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import * as DocumentPicker from 'expo-document-picker';
import { useAuth } from '../context/AuthContext';
import { Colors } from '../constants/theme';
import { GlassCard } from '../components/GlassCard';
import axios from 'axios';

export default function DocumentUploader() {
  const { token, apiBaseUrl } = useAuth();
  const router = useRouter();
  const [selectedFile, setSelectedFile] = useState<DocumentPicker.DocumentPickerAsset | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const colorScheme = useColorScheme() ?? 'dark';
  const colors = Colors[colorScheme];

  const pickDocument = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: '*/*',
        copyToCacheDirectory: true,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        const file = result.assets[0];
        const extension = file.name.split('.').pop()?.toLowerCase();
        
        if (
          extension !== 'pdf' && 
          extension !== 'xlsx' && 
          extension !== 'xls' && 
          extension !== 'txt'
        ) {
          Alert.alert('Unsupported Format', 'Please upload a PDF, Excel (.xlsx/.xls), or Text (.txt) file.');
          return;
        }
        
        setSelectedFile(file);
      }
    } catch (err) {
      console.error('Error picking document', err);
      Alert.alert('Error', 'Failed to pick a document.');
    }
  };

  const uploadFile = async () => {
    if (!selectedFile) return;
    setIsUploading(true);

    const formData = new FormData();
    formData.append('file', {
      uri: selectedFile.uri,
      name: selectedFile.name,
      type: selectedFile.mimeType || 'application/octet-stream',
    } as any);

    try {
      const response = await axios.post(`${apiBaseUrl}/api/files/parse`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      if (response.data?.success && response.data.data) {
        const extractedText = response.data.data.extractedText;
        Alert.alert(
          'Upload Successful',
          'Your file was parsed! Let\'s hand it over to your AI Coach to review and refine.',
          [
            {
              text: 'Open AI Coach',
              onPress: () => {
                router.push({
                  pathname: '/chat',
                  params: { preloadedFileText: extractedText },
                });
                setSelectedFile(null);
              },
            },
          ]
        );
      } else {
        throw new Error(response.data?.message || 'Parsing failed');
      }
    } catch (e: any) {
      console.error('File upload failed', e);
      Alert.alert('Upload Failed', e.response?.data?.message || e.message || 'File upload failed.');
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: colors.text }]}>Routine Uploader</Text>
        <Text style={[styles.headerSubtitle, { color: colors.textSecondary }]}>
          Upload your existing daily diet plus workout PDF, TXT, or Excel routine.
        </Text>
      </View>

      <View style={styles.content}>
        <GlassCard style={styles.uploadZone} onPress={pickDocument}>
          <Text style={[styles.iconPlaceholder, { color: colors.accent }]}>⬆</Text>
          <Text style={[styles.uploadText, { color: colors.text }]}>
            {selectedFile ? 'Change Selected File' : 'Choose Document'}
          </Text>
          <Text style={[styles.uploadSub, { color: colors.textSecondary }]}>
            Supports PDF, Excel (.xlsx), TXT plans
          </Text>
        </GlassCard>

        {selectedFile && (
          <GlassCard style={styles.fileCard}>
            <View style={styles.fileDetails}>
              <Text style={[styles.fileName, { color: colors.text }]}>{selectedFile.name}</Text>
              {selectedFile.size && (
                <Text style={[styles.fileSize, { color: colors.textSecondary }]}>
                  {Math.round(selectedFile.size / 1024)} KB
                </Text>
              )}
            </View>
            <TouchableOpacity
              style={[styles.actionBtn, { backgroundColor: colors.accent }]}
              onPress={uploadFile}
              disabled={isUploading}
            >
              {isUploading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.actionBtnText}>Parse Routine</Text>
              )}
            </TouchableOpacity>
          </GlassCard>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    paddingHorizontal: 24,
    paddingTop: 64,
    paddingBottom: 16,
    gap: 4,
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    letterSpacing: -0.5,
  },
  headerSubtitle: {
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '500',
  },
  content: {
    padding: 24,
    gap: 20,
  },
  uploadZone: {
    height: 180,
    alignItems: 'center',
    justifyContent: 'center',
    borderStyle: 'dashed',
    borderWidth: 2,
    gap: 8,
  },
  iconPlaceholder: {
    fontSize: 32,
    fontWeight: 'bold',
  },
  uploadText: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  uploadSub: {
    fontSize: 11,
    fontWeight: '500',
  },
  fileCard: {
    padding: 16,
    gap: 16,
  },
  fileDetails: {
    gap: 4,
  },
  fileName: {
    fontSize: 14,
    fontWeight: 'bold',
  },
  fileSize: {
    fontSize: 11,
    fontWeight: '600',
  },
  actionBtn: {
    height: 44,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  actionBtnText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: 'bold',
  },
});
