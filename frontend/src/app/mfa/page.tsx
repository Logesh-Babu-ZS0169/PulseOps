'use client';

import React, { useState, useEffect } from 'react';
import {
  Box,
  Container,
  Paper,
  Typography,
  Button,
  TextField,
  Alert,
  Card,
  CardContent,
  Grid,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemText,
  Divider
} from '@mui/material';
import { Security, QrCode2, VpnKey, Download } from '@mui/icons-material';
import axios from 'axios';
import { useRouter } from 'next/navigation';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export default function MFAManagement() {
  const router = useRouter();
  const [mfaStatus, setMFAStatus] = useState<any>(null);
  const [enrollmentData, setEnrollmentData] = useState<any>(null);
  const [verificationCode, setVerificationCode] = useState('');
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showRecoveryCodes, setShowRecoveryCodes] = useState(false);

  useEffect(() => {
    fetchMFAStatus();
  }, []);

  const fetchMFAStatus = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_URL}/api/multi-factor-auth/status`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      setMFAStatus(response.data);
    } catch (err: any) {
      setError('Failed to load MFA status');
    }
  };

  const handleEnroll = async () => {
    setLoading(true);
    setError('');
    try {
      const token = localStorage.getItem('token');
      const response = await axios.post(`${API_URL}/api/multi-factor-auth/enroll`, {}, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      setEnrollmentData(response.data);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Enrollment failed');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyAndEnable = async () => {
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const token = localStorage.getItem('token');
      const response = await axios.post(`${API_URL}/api/multi-factor-auth/verify-and-enable`, 
        { code: parseInt(verificationCode) },
        { headers: { 'Authorization': `Bearer ${token}` } }
      );
      setSuccess(response.data.message);
      setEnrollmentData(null);
      setVerificationCode('');
      await fetchMFAStatus();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Verification failed');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateRecoveryCodes = async () => {
    setLoading(true);
    setError('');
    try {
      const token = localStorage.getItem('token');
      const response = await axios.post(`${API_URL}/api/multi-factor-auth/recovery-codes/generate`, {}, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      setRecoveryCodes(response.data.codes);
      setShowRecoveryCodes(true);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to generate recovery codes');
    } finally {
      setLoading(false);
    }
  };

  const handleDisableMFA = async () => {
    if (!confirm('Are you sure you want to disable MFA? This will reduce your account security.')) return;
    
    setLoading(true);
    setError('');
    try {
      const token = localStorage.getItem('token');
      await axios.post(`${API_URL}/api/multi-factor-auth/disable`, {}, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      setSuccess('MFA has been disabled');
      await fetchMFAStatus();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to disable MFA');
    } finally {
      setLoading(false);
    }
  };

  const downloadRecoveryCodes = () => {
    const text = recoveryCodes.join('\n');
    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', 'mfa-recovery-codes.txt');
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Button onClick={() => router.push('/')} sx={{ mb: 2 }}>
        ← Back to Dashboard
      </Button>

      <Typography variant="h4" sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 1 }}>
        <Security /> Multi-Factor Authentication
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>{success}</Alert>}

      <Grid container spacing={3}>
        {/* MFA Status Card */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>MFA Status</Typography>
              {mfaStatus && (
                <Box>
                  <Typography variant="body1" sx={{ mb: 1 }}>
                    Status: <Chip
                      label={mfaStatus.enabled ? 'Enabled' : 'Disabled'}
                      color={mfaStatus.enabled ? 'success' : 'warning'}
                      size="small"
                    />
                  </Typography>
                  {mfaStatus.enabled && (
                    <>
                      <Typography variant="body2" color="text.secondary">
                        Enabled: {mfaStatus.enabledAt ? new Date(mfaStatus.enabledAt).toLocaleString() : 'N/A'}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Last Used: {mfaStatus.lastUsedAt ? new Date(mfaStatus.lastUsedAt).toLocaleString() : 'Never'}
                      </Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                        Unused Recovery Codes: {mfaStatus.unusedRecoveryCodes || 0}
                      </Typography>
                    </>
                  )}
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Actions Card */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>Actions</Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {!mfaStatus?.enabled && (
                  <Button
                    variant="contained"
                    color="primary"
                    onClick={handleEnroll}
                    disabled={loading}
                    startIcon={<QrCode2 />}
                  >
                    Enable MFA
                  </Button>
                )}
                {mfaStatus?.enabled && (
                  <>
                    <Button
                      variant="contained"
                      color="primary"
                      onClick={handleGenerateRecoveryCodes}
                      disabled={loading}
                      startIcon={<VpnKey />}
                    >
                      Generate Recovery Codes
                    </Button>
                    <Button
                      variant="outlined"
                      color="error"
                      onClick={handleDisableMFA}
                      disabled={loading}
                    >
                      Disable MFA
                    </Button>
                  </>
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Enrollment Card */}
        {enrollmentData && (
          <Grid item xs={12}>
            <Paper sx={{ p: 3, background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
              <Typography variant="h6" sx={{ mb: 2, color: 'white' }}>
                Scan QR Code with Google Authenticator
              </Typography>
              <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                  <Box sx={{ textAlign: 'center', bgcolor: 'white', p: 2, borderRadius: 1 }}>
                    <img src={enrollmentData.qrCodeImage} alt="QR Code" style={{ maxWidth: '100%', height: 'auto' }} />
                    <Typography variant="caption" sx={{ display: 'block', mt: 1 }}>
                      {enrollmentData.username}@{enrollmentData.issuer}
                    </Typography>
                  </Box>
                  <Box sx={{ mt: 2, p: 2, bgcolor: 'rgba(255,255,255,0.1)', borderRadius: 1 }}>
                    <Typography variant="body2" sx={{ color: 'white', mb: 1, fontWeight: 'bold' }}>
                      Can't scan? Enter this key manually:
                    </Typography>
                    <Typography variant="body2" sx={{ 
                      color: 'white', 
                      fontFamily: 'monospace', 
                      fontSize: '0.9rem',
                      wordBreak: 'break-all',
                      bgcolor: 'rgba(0,0,0,0.2)',
                      p: 1,
                      borderRadius: 1
                    }}>
                      {enrollmentData.secretKey}
                    </Typography>
                    <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.8)', display: 'block', mt: 1 }}>
                      In Google Authenticator, tap "+" → "Enter a setup key" → paste this code
                    </Typography>
                  </Box>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="body1" sx={{ color: 'white', mb: 2 }}>
                    1. Install Google Authenticator or Authy on your mobile device
                  </Typography>
                  <Typography variant="body1" sx={{ color: 'white', mb: 2 }}>
                    2. Scan the QR code OR enter the secret key manually
                  </Typography>
                  <Typography variant="body1" sx={{ color: 'white', mb: 3 }}>
                    3. Enter the 6-digit code below to verify
                  </Typography>
                  <TextField
                    label="Verification Code"
                    value={verificationCode}
                    onChange={(e) => setVerificationCode(e.target.value)}
                    fullWidth
                    sx={{ mb: 2, bgcolor: 'white', borderRadius: 1 }}
                    inputProps={{ maxLength: 6, pattern: '[0-9]*' }}
                  />
                  <Button
                    variant="contained"
                    onClick={handleVerifyAndEnable}
                    disabled={loading || verificationCode.length !== 6}
                    fullWidth
                  >
                    Verify and Enable MFA
                  </Button>
                </Grid>
              </Grid>
            </Paper>
          </Grid>
        )}
      </Grid>

      {/* Recovery Codes Dialog */}
      <Dialog open={showRecoveryCodes} onClose={() => setShowRecoveryCodes(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          Recovery Codes
        </DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Save these codes in a secure location. Each code can only be used once.
          </Alert>
          <List>
            {recoveryCodes.map((code, index) => (
              <React.Fragment key={index}>
                <ListItem>
                  <ListItemText
                    primary={code}
                    primaryTypographyProps={{ fontFamily: 'monospace', fontSize: '1.1rem' }}
                  />
                </ListItem>
                {index < recoveryCodes.length - 1 && <Divider />}
              </React.Fragment>
            ))}
          </List>
        </DialogContent>
        <DialogActions>
          <Button onClick={downloadRecoveryCodes} startIcon={<Download />}>
            Download
          </Button>
          <Button onClick={() => setShowRecoveryCodes(false)} variant="contained">
            Close
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
