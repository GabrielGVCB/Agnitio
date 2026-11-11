export interface Photographer {
  id: string;
  email: string;
  name: string;
  role: 'ADMIN' | 'PHOTOGRAPHER';
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  expiresInSeconds: number;
  photographer?: Photographer;
}

export type EventType = 'RACE' | 'WEDDING' | 'GRADUATION' | 'CORPORATE' | 'OTHER';
export type EventVisibility = 'PUBLIC' | 'PRIVATE';
export type EventStatus = 'ACTIVE' | 'ARCHIVED';

export interface EventDto {
  id: string;
  photographerId: string;
  name: string;
  type: EventType;
  visibility: EventVisibility;
  hasAccessCode: boolean;
  biometricRetentionDays: number;
  status: EventStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEventInput {
  name: string;
  type: EventType;
  visibility: EventVisibility;
  accessCode?: string;
  biometricRetentionDays: number;
}

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

let authToken: string | null = null;

export const setAuthToken = (token: string | null) => {
  authToken = token;
  if (typeof window !== 'undefined') {
    if (token) {
      localStorage.setItem('agnitio_token', token);
    } else {
      localStorage.removeItem('agnitio_token');
    }
  }
};

export const getAuthToken = (): string | null => {
  if (!authToken && typeof window !== 'undefined') {
    authToken = localStorage.getItem('agnitio_token');
  }
  return authToken;
};

async function fetchClient<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    credentials: 'include', // sends refresh token cookie
  });

  if (!response.ok) {
    let errorMessage = `HTTP ${response.status}`;
    try {
      const errJson = await response.json();
      errorMessage = errJson.message || errJson.error || errorMessage;
    } catch {
      // ignore
    }
    throw new Error(errorMessage);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

export interface UploadUrlItem {
  photoId: string;
  fileName: string;
  uploadUrl: string;
  storageKey: string;
}

export interface PhotoGalleryItem {
  photoId: string;
  originalFileName: string;
  status: 'PENDING_UPLOAD' | 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED';
  originalUrl: string;
  previewUrl: string;
  thumbnailUrl: string;
  createdAt: string;
}

export interface EventGalleryResponse {
  eventId: string;
  totalPhotos: number;
  photos: PhotoGalleryItem[];
}

export interface SelfieMatchItem {
  photoId: string;
  previewUrl: string;
  thumbnailUrl: string;
  similarity: number;
}

export interface SearchSelfieResponse {
  eventId: string;
  totalMatches: number;
  matches: SelfieMatchItem[];
}

export interface EventProgressDto {
  total: number;
  processed: number;
  failed: number;
  completed: boolean;
  percentage: number;
}

export const api = {
  auth: {
    async register(data: { email: string; password: string; name: string }) {
      return fetchClient<Photographer>('/auth/register', {
        method: 'POST',
        body: JSON.stringify(data),
      });
    },

    async login(data: { email: string; password: string }) {
      const res = await fetchClient<AuthResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify(data),
      });
      setAuthToken(res.accessToken);
      return res;
    },

    async logout() {
      try {
        await fetchClient<void>('/auth/logout', { method: 'POST' });
      } finally {
        setAuthToken(null);
      }
    },

    async me() {
      return fetchClient<Photographer>('/auth/me');
    },

    async refresh() {
      const res = await fetchClient<AuthResponse>('/auth/refresh', { method: 'POST' });
      setAuthToken(res.accessToken);
      return res;
    },
  },

  events: {
    async list() {
      return fetchClient<EventDto[]>('/events');
    },

    async get(id: string) {
      return fetchClient<EventDto>(`/events/${id}`);
    },

    async getPublic(id: string, code?: string) {
      const query = code ? `?code=${encodeURIComponent(code)}` : '';
      return fetchClient<EventDto>(`/events/${id}/public${query}`);
    },

    async create(data: CreateEventInput) {
      return fetchClient<EventDto>('/events', {
        method: 'POST',
        body: JSON.stringify(data),
      });
    },

    async update(id: string, data: CreateEventInput) {
      return fetchClient<EventDto>(`/events/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data),
      });
    },

    async delete(id: string) {
      return fetchClient<void>(`/events/${id}`, {
        method: 'DELETE',
      });
    },
  },

  photos: {
    async requestUploadUrls(eventId: string, files: { fileName: string }[]) {
      return fetchClient<{ items: UploadUrlItem[] }>(`/events/${eventId}/photos/upload-urls`, {
        method: 'POST',
        body: JSON.stringify({ files }),
      });
    },

    async confirmUpload(eventId: string, photoIds: string[]) {
      return fetchClient<{ confirmedCount: number; confirmedPhotoIds: string[] }>(`/events/${eventId}/photos/confirm`, {
        method: 'POST',
        body: JSON.stringify({ photoIds }),
      });
    },

    async getGallery(eventId: string, accessCode?: string) {
      const query = accessCode ? `?accessCode=${encodeURIComponent(accessCode)}` : '';
      return fetchClient<EventGalleryResponse>(`/events/${eventId}/photos${query}`);
    },

    async getProgress(eventId: string) {
      return fetchClient<EventProgressDto>(`/events/${eventId}/progress/status`);
    },

    async searchBySelfie(eventId: string, file: File | Blob, consent: boolean): Promise<SearchSelfieResponse> {
      const formData = new FormData();
      formData.append('selfie', file, 'selfie.jpg');
      formData.append('consent', String(consent));

      const response = await fetch(`${API_BASE}/events/${eventId}/photos/search-by-selfie`, {
        method: 'POST',
        body: formData,
        credentials: 'include',
      });

      if (!response.ok) {
        let errorMessage = `HTTP ${response.status}`;
        try {
          const errJson = await response.json();
          errorMessage = errJson.message || errJson.error || errorMessage;
        } catch {
          // ignore
        }
        throw new Error(errorMessage);
      }

      return response.json();
    },

    getProgressEventSourceUrl(eventId: string): string {
      return `${API_BASE}/events/${eventId}/progress`;
    },

    uploadDirectToS3(uploadUrl: string, file: File, onProgress?: (percentage: number) => void): Promise<void> {
      return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('PUT', uploadUrl, true);
        xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream');

        if (xhr.upload && onProgress) {
          xhr.upload.onprogress = (event) => {
            if (event.lengthComputable) {
              const percent = Math.round((event.loaded / event.total) * 100);
              onProgress(percent);
            }
          };
        }

        xhr.onload = () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            resolve();
          } else {
            reject(new Error(`S3 upload failed with status ${xhr.status}`));
          }
        };

        xhr.onerror = () => reject(new Error('Network error during S3 upload'));
        xhr.ontimeout = () => reject(new Error('Timeout during S3 upload'));
        xhr.send(file);
      });
    },
  },
};
