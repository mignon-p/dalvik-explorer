#include "config.h"

#include <boost/algorithm/string.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/filesystem.hpp>      // -lboost_filesystem.
#include <boost/program_options.hpp> // -lboost_program_options.
#include <boost/thread.hpp>          // -lboost_thread
#include <id3/misc_support.h>        // ID3 tags -lid3.
#include <microhttpd.h>              // HTTP server -lmicrohttpd.

#include <deque>
#include <iostream>
#include <map>
#include <string>
#include <vector>

#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

//
// typedefs.
//
typedef boost::filesystem::path Path;
typedef std::vector<boost::filesystem::path> Paths;
typedef std::vector<std::string> StringVector;
typedef std::map<std::string, std::string> StringStringMap;

//
// Configurable options.
//
static bool mp3d_debug_httpd(false);
static std::string mp3d_music_root("/home/elliotth/Music/Amazon");
static int mp3d_port(8888);

//
// Represents a single MP3.
//
struct Mp3Info {
  std::string title;
  std::string artist;
  std::string album;

  std::string filename;
  
  size_t id;
  
  bool matches(const std::string& s) const {
    StringVector tokens;
    boost::split(tokens, s, boost::is_any_of(" "));
    
    StringVector::const_iterator it;
    for (it = tokens.begin(); it != tokens.end(); ++it) {
      if (!boost::icontains(title, *it) &&
          !boost::icontains(artist, *it) &&
          !boost::icontains(album, *it)) {
        return false;
      }
    }
    return true;
  }
  
  void dumpTo(std::ostream& os) const {
    os << "Mp3Info['" << title << "'"
       << " by '" << artist << "'"
       << " from '" << album << "' (" << filename << ")]";
  }
};

std::ostream& operator<<(std::ostream& os, const Mp3Info& rhs) {
  rhs.dumpTo(os);
  return os;
}

//
// A thread-safe queue of MP3s to be played.
//
class PlayQueue {
public:
  void push_back(const Mp3Info& mp3) {
    boost::mutex::scoped_lock lock(mutex);
    mp3s.push_back(mp3);
    non_empty.notify_one();
  }
  
  // Blocks until an element is available.
  Mp3Info pop_front() {
    boost::mutex::scoped_lock lock(mutex);
    while (mp3s.empty()) {
      non_empty.wait(lock);
    }
    Mp3Info result(mp3s.front());
    mp3s.pop_front();
    return result;
  }
  
  void remove(size_t id) {
    boost::mutex::scoped_lock lock(mutex);
    typedef std::deque<Mp3Info>::iterator It;
    for (It it = mp3s.begin(); it != mp3s.end(); ++it) {
      if (it->id == id) {
        mp3s.erase(it);
        return;
      }
    }
  }
  
  void clear() {
    boost::mutex::scoped_lock lock(mutex);
    mp3s.clear();
  }
  
  std::vector<Mp3Info> as_vector() const {
    boost::mutex::scoped_lock lock(mutex);
    std::vector<Mp3Info> result(mp3s.begin(), mp3s.end());
    return result;
  }
  
private:
  std::deque<Mp3Info> mp3s;
  
  mutable boost::mutex mutex;
  boost::condition non_empty;
};

/*
class Process {
public:
  Process() : pid(0) {
    
  }
  
  ~Process() {
    kill();
  }
  
  void kill() {
    if (pid == 0) {
      return;
    }
    if (kill(pid, SIGINT) == -1) {
      std::cerr << "kill(" << pid << ", SIGINT) failed: " << error_string();
    }
    pid = 0;
  }
  
private:
  pid_t pid;
};
*/

//
// Global data.
//
static PlayQueue play_queue; // FIXME

static boost::mutex now_playing_mutex;
static Mp3Info* now_playing; // FIXME

static std::vector<Mp3Info> all_mp3s; // FIXME

static StringStringMap static_file_map; // FIXME

static void fail(const std::string& msg) {
  std::cerr << msg << std::endl;
  exit(EXIT_FAILURE);
}

static std::string error_string() {
  char buf[BUFSIZ];
  return strerror_r(errno, buf, sizeof(buf));
}

static void mp3_play_loop() {
  while (true) {
    Mp3Info mp3(play_queue.pop_front());
    {
      boost::mutex::scoped_lock lock(now_playing_mutex);
      now_playing = &mp3;
    }
    
    //FILE* fp = popen("mplayer -slave -really-quiet", "w");
    
    std::ostringstream command;
    command << "mplayer '" << mp3.filename << "'";
    system(command.str().c_str());
    
    {
      boost::mutex::scoped_lock lock(now_playing_mutex);
      now_playing = 0;
    }
  }
}

static void make_mp3_table(std::ostream& os, const std::vector<Mp3Info>& mp3s, const char* q, bool is_queue) {
  os << "<table class='sortable' id='playlist' width='100%'>\n";
  os << " <thead>\n";
  os << "  <tr> <th>&nbsp;</th> <th>Title</th> <th>Artist</th> <th>Album</th> </tr>\n";
  os << " </thead>\n";
  os << " <tbody>\n";
  size_t row_count = 0;
  for (size_t i = 0; i < mp3s.size(); ++i) {
    const Mp3Info& mp3(mp3s[i]);
    if (q && !mp3.matches(q)) {
      continue;
    }
    os << "  <tr> ";
    os << "<td>";
    if (is_queue) {
      os << "<a href='/remove/" << mp3.id << "'><img src='/static/remove.png'></a>";
    } else {
      os << "<a href='/add/" << mp3.id << "'><img src='/static/add.png'></a>"
         << "<a href='/play/" << mp3.id << "'><img src='/static/play.png'></a>";
    }
    os << "</td>";
    // FIXME: html escape these strings!
    os << "<td>" << mp3.title << "</td>";
    os << "<td>" << mp3.artist << "</td>";
    os << "<td>" << mp3.album << "</td>";
    os << "</tr>\n";
    ++row_count;
  }
  if (row_count == 0) {
    os << "  <tr><td></td> <td>(Nothing.)</td> <td></td> <td></td> </tr>\n";
  }
  os << " </tbody>\n";
  os << "</table>\n";
}

static void make_main_page(std::string& result, const char* q) {
  std::ostringstream page;
  page << "<html>";
  page << "<head>";
  page << " <title>" PACKAGE_STRING "</title>";
  page << " <link rel='stylesheet' href='/static/site.css' type='text/css'>";
  // FIXME: make our own copy, make it case-insensitive, and serve "/static/" stuff (like this and the CSS) from disk.
  // FIXME: is this fast enough on Firefox 3 and Chrome? it's too slow with Camino (brings up a warning dialog).
  //page << " <script src='http://www.kryogenix.org/code/browser/sorttable/sorttable.js'></script>";
  page << "</head>";
  page << "<body>";
  
  // Are we playing anything at the moment?
  Mp3Info current;
  {
    boost::mutex::scoped_lock lock(now_playing_mutex);
    if (now_playing) {
      current = *now_playing;
    }
  }
  if (!current.filename.empty()) {
    page << "<h1>Now Playing</h1>\n";
    page << "<p>\"" << current.title << "\" by " << current.artist << " from " << current.album << ".\n";
  }
  
  page << "<h1>Play Queue</h1>\n";
  make_mp3_table(page, play_queue.as_vector(), NULL, true);
  
  page << "<h1>Search Library</h1>\n";
  page << "<form>";
  page << "<input name='mp3d_q' type='text' value='" << (q ? q : "") << "'>";
  page << "</form>\n";
  
  make_mp3_table(page, all_mp3s, q, false);

  page << "<div id='footer'>"
       << "<p>Copyright &copy; 2009 <a href='http://www.jessies.org/~enh/'>Elliott Hughes</a>.</p>"
       << "<p>" << PACKAGE_STRING << ".</p>"
       << "</div>";

  page << "</body>";
  page << "</html>";

  result = page.str();
}

static std::string make_css() {
  std::string result;
  // Mostly copied from RhythmWeb, which looks nice, though it's unusably slow.
  result += "html, body { color: WindowText; background: Window; font: caption; }\n";
  result += "a img { border: 0px; vertical-align: middle; }\n";
  result += "#footer { margin-top: 24px; }\n";
  result += "#footer p { margin: 0px; text-align: center; font-size: 0.8em; }\n";
  result += "table { margin-top: 8px; border-collapse: separate; border-spacing: 0px; padding: 1px; background: ThreeDShadow; }\n";
  result += "table thead th { border-top: 1px solid ThreeDHighlight; border-left: 1px solid ThreeDShadow; border-bottom: 1px solid ThreeDShadow; padding: 4px; font-size: 1em; text-align: left; color: ButtonText; background: ButtonFace; }\n";
  result += "table thead th:first-child { border-left: 0px; }\n";
  result += "table tbody td { padding: 4px; color: ButtonText; background: ButtonFace; }\n";
  return result;
}

static int send_string_response(MHD_Connection* connection, int code, const std::string& s) {
  MHD_Response* response = MHD_create_response_from_data(s.size(), const_cast<char*>(s.data()), MHD_NO, MHD_YES);
  MHD_queue_response(connection, code, response);
  MHD_destroy_response(response);
  return MHD_YES;
}

static int report_error(MHD_Connection* connection, int code, const std::string& text) {
  const std::string error = "<html><body><h1>" + text + "</h1></body></html>";
  return send_string_response(connection, code, error);
}

static int see_other(MHD_Connection* connection, const std::string& new_location) {
  MHD_Response* response = MHD_create_response_from_data(0, 0, MHD_NO, MHD_NO);
  MHD_add_response_header(response, MHD_HTTP_HEADER_LOCATION, new_location.c_str());
  MHD_queue_response(connection, MHD_HTTP_SEE_OTHER, response);
  MHD_destroy_response(response);
  return MHD_YES;
}

static int dump_key(void*, MHD_ValueKind, const char* key, const char* value) {
  std::cout << key << " = " << value << std::endl;
  return MHD_YES;
}

static int handle_request(void* /*cls*/, MHD_Connection* connection, const char* url, const char* method, const char*, const char*, unsigned int*, void** /*con_cls*/) {
  if (std::string(method) != "GET") {
    return report_error(connection, MHD_HTTP_NOT_IMPLEMENTED, "501 Not Implemented");
  }
  
  if (mp3d_debug_httpd) {
    MHD_get_connection_values(connection, MHD_HEADER_KIND, dump_key, NULL);
    MHD_get_connection_values(connection, MHD_COOKIE_KIND, dump_key, NULL);
    MHD_get_connection_values(connection, MHD_GET_ARGUMENT_KIND, dump_key, NULL);
    std::cout << "*** " << method << " request for '" << url << "'" << std::endl;
  }
  
  const char* q = MHD_lookup_connection_value(connection, MHD_GET_ARGUMENT_KIND, "mp3d_q");
  
  const std::string request_url(url);
  if (request_url == "/") {
    std::string page_data;
    make_main_page(page_data, q);
    return send_string_response(connection, MHD_HTTP_OK, page_data);
  } else if (boost::starts_with(request_url, "/static/")) {
    StringStringMap::const_iterator it = static_file_map.find(request_url);
    if (it == static_file_map.end()) {
      return report_error(connection, MHD_HTTP_NOT_FOUND, "404 Not Found");
    }
    return send_string_response(connection, MHD_HTTP_OK, it->second);
  } else if (boost::starts_with(request_url, "/play/")) {
    const size_t id = strtoul(request_url.substr(6).c_str(), 0, 10); // FIXME: error checking.
    play_queue.clear();
    play_queue.push_back(all_mp3s[id]); // FIXME: lookup by id (don't assume id == index).
    return see_other(connection, "/");
  } else if (boost::starts_with(request_url, "/add/")) {
    const size_t id = strtoul(request_url.substr(5).c_str(), 0, 10); // FIXME: error checking.
    play_queue.push_back(all_mp3s[id]); // FIXME: lookup by id (don't assume id == index).
    return see_other(connection, "/");
  } else if (boost::starts_with(request_url, "/remove/")) {
    const size_t id = strtoul(request_url.substr(8).c_str(), 0, 10); // FIXME: error checking.
    play_queue.remove(id); // FIXME: is this set-like behavior the behavior we want?
    return see_other(connection, "/");
  } else {
    return report_error(connection, MHD_HTTP_NOT_FOUND, "404 Not Found");
  }
}

static void find_mp3_files(const Path& dir_path, Paths& paths) {
  if (!exists(dir_path)) {
    return;
  }
  boost::filesystem::directory_iterator it(dir_path);
  const boost::filesystem::directory_iterator end_it;
  for (; it != end_it; ++it) {
    if (is_directory(it->status())) {
      find_mp3_files(it->path(), paths);
    } else if (boost::ends_with(it->path().string(), ".mp3")) {
      paths.push_back(it->path());
    }
  }
}

static void read_static_file(const std::string& url, const std::string& disk_path) {
  const int fd = open(disk_path.c_str(), O_RDONLY);
  if (fd == -1) {
    fail("Unable to open '" + disk_path + "' for reading: " + error_string());
  }
  std::string data;
  char buf[BUFSIZ];
  int n;
  while ((n = TEMP_FAILURE_RETRY(read(fd, buf, sizeof(buf)))) > 0) {
    data.append(buf, n);
  }
  close(fd);
  if (n == -1) {
    fail("Failed read of '" + disk_path + "': " + error_string());
  }
  static_file_map[url] = data;
}

int main(int argc, char* argv[]) {
  // Parse any command-line options.
  namespace po = boost::program_options;
  po::options_description desc("Allowed options");
  desc.add_options()
    ("help", "show help")
    ("debug-httpd", po::value<bool>(&mp3d_debug_httpd), "show httpd debug output")
    ("root", po::value<std::string>(&mp3d_music_root), "root of file system mp3 tree")
    ("port", po::value<int>(&mp3d_port), "httpd port number")
  ;
  po::variables_map args;
  po::store(po::parse_command_line(argc, argv, desc), args);
  po::notify(args);
  if (args.count("help")) {
    std::cout << desc << std::endl;
    return 1;
  }

  // Index all the mp3s.
  Paths paths;
  find_mp3_files(mp3d_music_root, paths);
  std::cerr << ".mp3 files found: " << paths.size() << std::endl;

  int old_percentage = -1;
  size_t id = 0;
  for (Paths::const_iterator it = paths.begin(); it != paths.end(); ++it) {
    Mp3Info mp3;
    mp3.filename = (*it).string();

    const ID3_Tag tag(mp3.filename.c_str());

    ID3_Tag::ConstIterator* it = tag.CreateIterator();
    for (size_t i = 0; i < tag.NumFrames(); ++i) {
      const ID3_Frame* frame = it->GetNext();
      if (frame != 0) {
        std::string* dst;
        switch (frame->GetID()) {
        case ID3FID_ALBUM: dst = &mp3.album; break;
        case ID3FID_LEADARTIST: dst = &mp3.artist; break;
        case ID3FID_TITLE: dst = &mp3.title; break;
        default: continue;
        }
        char* text = ID3_GetString(frame, ID3FN_TEXT);
        dst->assign(text);
        ID3_FreeString(text);
      }
    }
    
    // FIXME: maybe a hash, to enable bookmarks?
    mp3.id = id++;
    
    all_mp3s.push_back(mp3);
    
    // Show progress. Not really useful when we're not debugging.
    // FIXME: start the web server straight away, and say "Indexing..." there.
    const int new_percentage = (100*all_mp3s.size())/paths.size();
    if (new_percentage != old_percentage) {
      std::cout << "\rScanned: " << new_percentage << "%" << std::flush;
      old_percentage = new_percentage;
    }
  }
  std::cout << "\r.mp3 files scanned: " << all_mp3s.size() << std::endl;

  // Set up the static files we need to serve.
  read_static_file("/static/add.png",
                   "/usr/share/icons/gnome/16x16/actions/gtk-add.png");
  read_static_file("/static/play.png",
                   "/usr/share/icons/gnome/16x16/actions/gtk-media-play-ltr.png");
  read_static_file("/static/remove.png",
                   "/usr/share/icons/gnome/16x16/actions/gtk-remove.png");
  static_file_map["/static/site.css"] = make_css();
  
  // Start the mp3 player thread.
  boost::thread mp3_player_thread(mp3_play_loop);
  
  // Start the HTTP server.
  std::cerr << "Starting HTTP server on port " << mp3d_port << "..." << std::endl;
  const int mhd_flags = MHD_USE_SELECT_INTERNALLY;
  MHD_Daemon* daemon = MHD_start_daemon(mhd_flags, mp3d_port, 0, 0, &handle_request, 0, MHD_OPTION_END);
  if (daemon == 0) {
    fail("MHD_start_daemon failed!");
  }
  
  getchar(); // Wait for the user to hit enter.
  
  MHD_stop_daemon(daemon);
  //mp3_player_thread.join();
  return 0;
}
