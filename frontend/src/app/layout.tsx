import type { Metadata } from "next";
import {
  Geist_Mono,
  Maven_Pro,
  Noto_Sans_Khmer,
  Noto_Sans_KR,
} from "next/font/google";
import { routing } from "@/i18n/routing";
import { ThemeScript } from "@/components/theme-script";
import "./globals.css";

const mavenPro = Maven_Pro({
  variable: "--font-maven",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const notoKhmer = Noto_Sans_Khmer({
  variable: "--font-noto-khmer",
  subsets: ["khmer"],
  weight: ["400", "500", "600", "700"],
});

const notoKr = Noto_Sans_KR({
  variable: "--font-noto-kr",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

export const metadata: Metadata = {
  title: {
    default: "Clipzy",
    template: "%s · Clipzy",
  },
  description: "Upload, transcode, and stream video on Clipzy.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang={routing.defaultLocale}
      suppressHydrationWarning
      className={`${mavenPro.variable} ${geistMono.variable} ${notoKhmer.variable} ${notoKr.variable} h-full`}
    >
      <body className="flex min-h-full flex-col font-sans">
        <ThemeScript />
        {children}
      </body>
    </html>
  );
}
