import './globals.css'
import type { Metadata } from 'next'
import MUIThemeProvider from './theme-provider'
import { AuthProvider } from '../contexts/AuthContext'

export const metadata: Metadata = {
  title: 'Inbound Ingestion Metrics Dashboard',
  description: 'Real-time monitoring of inbound document ingestion',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body>
        <MUIThemeProvider>
          <AuthProvider>
            {children}
          </AuthProvider>
        </MUIThemeProvider>
      </body>
    </html>
  )
}
