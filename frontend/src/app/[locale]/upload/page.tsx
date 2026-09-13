"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { api, putFileWithProgress } from "@/lib/api";
import { useAuth } from "@/components/auth-provider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { Link, useRouter } from "@/i18n/navigation";

type Phase =
  | "idle"
  | "creating"
  | "uploading"
  | "completing"
  | "processing"
  | "ready"
  | "failed";

export default function UploadPage() {
  const t = useTranslations("upload");
  const tCommon = useTranslations("common");
  const { isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();
  const fileRef = useRef<HTMLInputElement>(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [visibility, setVisibility] = useState("PUBLIC");
  const [file, setFile] = useState<File | null>(null);
  const [phase, setPhase] = useState<Phase>("idle");
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [videoId, setVideoId] = useState<string | null>(null);
  const [status, setStatus] = useState<string | null>(null);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.replace("/login?next=/upload");
    }
  }, [authLoading, isAuthenticated, router]);

  useEffect(() => {
    if (phase !== "processing" || !videoId) return;
    let cancelled = false;
    const poll = async () => {
      try {
        const v = await api.video(videoId);
        if (cancelled) return;
        setStatus(v.status);
        const s = (v.status || "").toUpperCase();
        if (s === "READY") {
          setPhase("ready");
          return;
        }
        if (s === "FAILED" || s === "ERROR") {
          setPhase("failed");
          setError(t("failed"));
          return;
        }
      } catch {
        /* keep polling */
      }
      if (!cancelled) {
        window.setTimeout(poll, 2500);
      }
    };
    void poll();
    return () => {
      cancelled = true;
    };
  }, [phase, videoId, t]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file || !title.trim()) return;
    setError(null);
    try {
      setPhase("creating");
      const created = await api.createVideo({
        title: title.trim(),
        description: description.trim() || undefined,
        visibility,
      });
      setVideoId(created.id);

      setPhase("uploading");
      setProgress(0);
      await putFileWithProgress(created.uploadUrl, file, setProgress);

      setPhase("completing");
      await api.completeUpload(created.id);

      setPhase("processing");
      setStatus("PROCESSING");
    } catch (err) {
      setPhase("failed");
      setError(err instanceof Error ? err.message : t("failed"));
    }
  };

  if (authLoading || !isAuthenticated) {
    return (
      <div className="mx-auto max-w-lg space-y-4">
        <Skeleton className="h-8 w-40" />
        <Skeleton className="h-40 w-full rounded-xl" />
        <p className="text-sm text-muted-foreground">{t("loginRequired")}</p>
      </div>
    );
  }

  if (phase === "ready" && videoId) {
    return (
      <div className="mx-auto max-w-lg space-y-4 rounded-2xl border border-border bg-card/60 p-8 text-center animate-fade-up">
        <h1 className="text-2xl font-semibold">{t("ready")}</h1>
        <Button asChild>
          <Link href={`/watch/${videoId}`}>{t("ready")}</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg space-y-6 animate-soft-in">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>
        <p className="text-sm text-muted-foreground">{t("subtitle")}</p>
      </div>

      <form
        onSubmit={onSubmit}
        className="space-y-4 rounded-2xl border border-border bg-card/50 p-6"
      >
        <div className="space-y-2">
          <Label htmlFor="title">{t("titleLabel")}</Label>
          <Input
            id="title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            disabled={phase !== "idle" && phase !== "failed"}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="description">{t("descriptionLabel")}</Label>
          <Textarea
            id="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={4}
            disabled={phase !== "idle" && phase !== "failed"}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="visibility">{t("visibilityLabel")}</Label>
          <select
            id="visibility"
            className="flex h-9 w-full rounded-lg border border-input bg-background px-3 text-sm"
            value={visibility}
            onChange={(e) => setVisibility(e.target.value)}
            disabled={phase !== "idle" && phase !== "failed"}
          >
            <option value="PUBLIC">{tCommon("public")}</option>
            <option value="UNLISTED">{tCommon("unlisted")}</option>
            <option value="PRIVATE">{tCommon("private")}</option>
          </select>
        </div>
        <div className="space-y-2">
          <Label htmlFor="file">{t("fileLabel")}</Label>
          <Input
            id="file"
            ref={fileRef}
            type="file"
            accept="video/*"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            disabled={phase !== "idle" && phase !== "failed"}
            required
          />
        </div>

        {(phase === "uploading" ||
          phase === "creating" ||
          phase === "completing" ||
          phase === "processing") && (
          <div className="space-y-2 rounded-xl bg-muted/60 p-4">
            <p className="text-sm font-medium">
              {phase === "processing"
                ? `${t("processing")}${status ? ` · ${status}` : ""}`
                : phase === "uploading"
                  ? t("uploading")
                  : phase}
            </p>
            {phase === "uploading" && (
              <div className="h-2 overflow-hidden rounded-full bg-background">
                <div
                  className="h-full bg-primary transition-[width] duration-200"
                  style={{ width: `${progress}%` }}
                />
              </div>
            )}
            {phase === "uploading" && (
              <p className="text-xs text-muted-foreground">{progress}%</p>
            )}
          </div>
        )}

        {error && <p className="text-sm text-destructive">{error}</p>}

        <Button
          type="submit"
          className="w-full"
          disabled={
            !file || !title.trim() || (phase !== "idle" && phase !== "failed")
          }
        >
          {phase === "idle" || phase === "failed" ? t("submit") : "…"}
        </Button>
      </form>
    </div>
  );
}
